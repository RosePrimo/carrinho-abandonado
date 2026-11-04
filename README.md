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
   X minutos, marca como `ABANDONADO` e **envia um lembrete** (simulado no log).

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
├── repository   → CarrinhoRepository (Spring Data JPA)
├── dto          → objetos de entrada e saída da API (records com validação)
├── model        → Carrinho, ItemCarrinho e StatusCarrinho
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

- **Job em várias instâncias:** com duas instâncias, os dois jobs podem pegar o mesmo carrinho e enviar
  o lembrete em dobro. A correção planejada é `SELECT ... FOR UPDATE SKIP LOCKED`, para cada instância
  pular os carrinhos que a outra já pegou.
- Enviar o lembrete por fila (Kafka), em vez de dentro do job.
- Testes automatizados das regras do carrinho.