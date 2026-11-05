# Carrinho Abandonado

API de carrinho de compras com detecção automática de **carrinhos abandonados**.
Quando um cliente coloca produtos no carrinho e some sem finalizar a compra, um job agendado
marca o carrinho como abandonado e envia um lembrete para ele voltar.

## Tecnologias

Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Docker Compose

## Como funciona

1. O cliente **cria um carrinho** e **adiciona produtos**.
2. Cada alteração no carrinho atualiza o campo `atualizado_em`.
3. Se o cliente **finaliza a compra**, o carrinho vira `FINALIZADO`.
4. A cada 30 segundos, um **job agendado** procura carrinhos `ATIVO` sem alteração há mais de
   X minutos, marca como `ABANDONADO` e **envia um lembrete** (simulado e registrado no banco).

```
ATIVO ──── finalizar ────► FINALIZADO
  │
  └── parado há X minutos (job) ──► ABANDONADO + lembrete
```

## Arquitetura (organizada por camada)

```
com.rose.carrinho
├── controller   → CarrinhoController (HTTP) e ApiExceptionHandler (erros → 409)
├── service      → CarrinhoService (regras e transações) e LembreteService (envio do lembrete)
├── repository   → CarrinhoRepository e LembreteRepository (Spring Data JPA)
├── dto          → objetos de entrada e saída da API (records com validação)
├── model        → Carrinho, ItemCarrinho, StatusCarrinho e Lembrete
└── job          → AbandonoCarrinhoJob (@Scheduled)
```

## Modelo de dados

Um carrinho tem vários itens (relacionamento **1 para N**):

```
carrinho                         item_carrinho
├── id                           ├── id
├── cliente_id                   ├── carrinho_id  (FK → carrinho.id)
├── status                       ├── produto
├── atualizado_em                ├── quantidade
└── lembrete_enviado             └── preco_unitario

lembrete
├── id
├── carrinho_id
├── cliente_id
├── instancia     (qual instância da API enviou)
└── enviado_em
```

- `@OneToMany` com `cascade = ALL` e `orphanRemoval = true`: o carrinho é o dono dos itens.
  Salvar o carrinho salva os itens; remover um item da lista apaga ele do banco.
- Índice em `(status, atualizado_em)`: a busca do job não varre a tabela inteira.
- Preços com `BigDecimal`, nunca `double`.

## Regras de negócio

- Cada cliente tem **no máximo um carrinho ativo**. Criar de novo devolve o mesmo carrinho.
- Adicionar um produto que já está no carrinho **soma a quantidade**.
- Só é possível alterar um carrinho `ATIVO`.
- Um carrinho **vazio não pode ser finalizado**.
- Carrinho vazio abandonado **não recebe lembrete**.
- O job processa **no máximo 100 carrinhos por rodada**, para não carregar tudo na memória.

## Endpoints

| Método | Caminho | Descrição | Resposta |
|---|---|---|---|
| POST | `/carrinhos` | Cria (ou devolve) o carrinho ativo do cliente | 201 |
| GET | `/carrinhos/{id}` | Busca o carrinho com os itens e o total | 200 · 404 |
| POST | `/carrinhos/{id}/itens` | Adiciona um produto | 200 · 400 · 409 |
| DELETE | `/carrinhos/{id}/itens/{itemId}` | Remove um produto | 200 · 409 |
| POST | `/carrinhos/{id}/finalizar` | Finaliza a compra | 200 · 409 |

## Job em várias instâncias: o problema e a correção

Com duas instâncias da API, os dois jobs agendados liam os mesmos carrinhos ao mesmo tempo
e enviavam o lembrete em dobro: o status `ABANDONADO` só era gravado no fim da rodada, e até
lá a outra instância ainda enxergava os carrinhos como `ATIVO`.

| | Sem trava | Com `FOR UPDATE SKIP LOCKED` |
|---|---|---|
| Carrinhos | 20 | 20 |
| Lembretes enviados | 33 (13 duplicados) | 20 |
| Clientes avisados 2 vezes | 13 | 0 |

**Correção:** a busca do job usa `SELECT ... FOR UPDATE SKIP LOCKED`. Cada instância tranca os
carrinhos que pegou, e a outra pula as linhas trancadas. Cada carrinho é processado por uma
instância só, e as instâncias dividem o trabalho.

```sql
select * from carrinho
where status = 'ATIVO' and atualizado_em < :limite
order by atualizado_em
limit 100
for update skip locked
```

Cada lembrete é gravado na tabela `lembrete`, com a instância que enviou, o que permite
conferir duplicidades com SQL:

```sql
select cliente_id, count(*) as vezes, string_agg(instancia, ' + ') as instancias
from lembrete
group by cliente_id
having count(*) > 1;
```

## Configuração

```yaml
carrinho:
  abandono:
    minutos: 2           # tempo sem alteração para considerar abandonado
    intervalo-ms: 30000  # de quanto em quanto tempo o job roda
```

## Como rodar

```bash
./mvnw clean package -DskipTests
docker compose up -d --build
```

Sobem o PostgreSQL e duas instâncias da API: `http://localhost:8081` e `http://localhost:8082`.

```bash
# criar carrinho
curl -X POST http://localhost:8081/carrinhos -H "Content-Type: application/json" -d '{"clienteId":"rose"}'

# adicionar produto
curl -X POST http://localhost:8081/carrinhos/1/itens -H "Content-Type: application/json" \
  -d '{"produto":"Fone Bluetooth","quantidade":1,"precoUnitario":199.90}'

# finalizar
curl -X POST http://localhost:8081/carrinhos/1/finalizar
```

## Próximos passos

- Marcar como abandonado e confirmar a transação antes de enviar o lembrete, para não segurar a
  trava durante o envio (padrão Outbox ou fila com Kafka).
- Testes automatizados das regras do carrinho.