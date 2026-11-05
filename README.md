# Carrinho Abandonado

![CI](https://github.com/RosePrimo/carrinho-abandonado/actions/workflows/ci.yml/badge.svg)

API de carrinho de compras com detecção automática de **carrinhos abandonados**.
Quando um cliente coloca produtos no carrinho e some sem finalizar a compra, um job agendado
marca o carrinho como abandonado e envia um lembrete para ele voltar.

## Tecnologias

Java 21 · Spring Boot 4 · Spring Data JPA · Spring Security (JWT) · PostgreSQL · Spring Actuator ·
Micrometer · Prometheus · JUnit 5 · Mockito · Testcontainers · Docker Compose · GitHub Actions

## Como funciona

1. O cliente **faz login** e recebe um **token JWT**.
2. Com o token, ele **cria um carrinho** e **adiciona produtos**.
3. Cada alteração no carrinho atualiza o campo `atualizado_em`.
4. Se o cliente **finaliza a compra**, o carrinho vira `FINALIZADO`.
5. A cada 30 segundos, um **job agendado** procura carrinhos `ATIVO` sem alteração há mais de
   X minutos, marca como `ABANDONADO` e **envia um lembrete** (simulado e registrado no banco).

```
ATIVO ──── finalizar ────► FINALIZADO
  │
  └── parado há X minutos (job) ──► ABANDONADO + lembrete
```

## Arquitetura (organizada por camada)

```
com.rose.carrinho
├── controller   → CarrinhoController, AuthController (login) e ApiExceptionHandler (erros)
├── service      → CarrinhoService (regras e transações) e LembreteService (envio do lembrete)
├── repository   → CarrinhoRepository e LembreteRepository (Spring Data JPA)
├── dto          → objetos de entrada e saída da API (records com validação)
├── model        → Carrinho, ItemCarrinho, StatusCarrinho e Lembrete
├── security     → SecurityConfig (regras de acesso, JWT) e TokenService (gera o token)
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

## Segurança (JWT)

- `POST /auth/login` valida usuário e senha (BCrypt) e devolve um **token JWT** assinado com
  **HS256**, válido por 1 hora.
- Todas as outras rotas exigem o cabeçalho `Authorization: Bearer <token>`. Sem token: **401**.
- O cliente vem do **subject do token**, nunca do corpo da requisição.
- **Isolamento por cliente:** cada operação confere se o carrinho é do usuário logado. Carrinho de
  outra pessoa responde **404**, e não 403, para não revelar que ele existe.
- Usuários de teste em memória: `rose` e `ana`, senha `123`.
- Em produção, a chave de assinatura ficaria num cofre de segredos (Secret Manager), e não no
  `application.yaml`.

## Endpoints

| Método | Caminho | Descrição | Resposta |
|---|---|---|---|
| POST | `/auth/login` | Login: devolve o token JWT | 200 · 401 |
| POST | `/carrinhos` | Cria (ou devolve) o carrinho ativo do cliente logado | 201 · 401 |
| GET | `/carrinhos/{id}` | Busca o carrinho com os itens e o total | 200 · 401 · 404 |
| POST | `/carrinhos/{id}/itens` | Adiciona um produto | 200 · 400 · 401 · 404 · 409 |
| DELETE | `/carrinhos/{id}/itens/{itemId}` | Remove um produto | 200 · 401 · 404 · 409 |
| POST | `/carrinhos/{id}/finalizar` | Finaliza a compra | 200 · 401 · 404 · 409 |
| GET | `/actuator/health` | Saúde da aplicação e do banco | 200 · 503 |
| GET | `/actuator/prometheus` | Métricas no formato do Prometheus | 200 |

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

## Observabilidade

### Actuator e métricas

- `/actuator/health` mostra a saúde da aplicação e do banco: `UP` (200) ou `DOWN` (503).
- O tempo das requisições é medido com **p50, p95 e p99**, por endpoint.
- **Métricas de negócio** próprias, com Micrometer:
    - `carrinho_abandonados_total`: carrinhos marcados como abandonados;
    - `carrinho_lembretes_enviados_total`: lembretes enviados.

### Falhar rápido quando o banco cai

Derrubando o banco, o health check levava **29 segundos** para responder, porque o pool de
conexões (HikariCP) espera 30 segundos por padrão. Com `connection-timeout: 3000`, a resposta
caiu para **3 segundos**, e o load balancer consegue tirar a instância do ar rapidamente.

| | Antes | Depois |
|---|---|---|
| Tempo do health check com o banco fora | 29 s | 3,2 s |

### Prometheus e alertas

O Prometheus coleta as métricas a cada 15 segundos e avalia as regras de `alertas.yml`:

| Alerta | Regra | Severidade |
|---|---|---|
| `AplicacaoFora` | O Prometheus não consegue ler a aplicação por 30 s | crítica |
| `TempoDeRespostaAlto` | p95 acima de 500 ms por 1 minuto | aviso |
| `MuitosCarrinhosAbandonados` | Mais de 5 abandonos em 10 minutos | aviso |

O painel fica em `http://localhost:9090` (alvos em `/targets`, alertas em `/alerts`).

## Testes

| Tipo | Classe | O que testa |
|---|---|---|
| Unidade | `CarrinhoTest` | As regras do carrinho, em Java puro |
| Unidade com Mockito | `CarrinhoServiceTest` | O service com o repository simulado, incluindo o isolamento por cliente |
| Integração | `CarrinhoApiTest` | A API inteira com MockMvc: login, token, 401, 404 e o fluxo da compra |
| Contexto | `CarrinhoApplicationTests` | A aplicação sobe sem erros |

Os testes de integração usam **Testcontainers**: cada execução sobe um PostgreSQL próprio num
container e descarta no final, sem depender de nenhum banco instalado.

```bash
./mvnw test
```

A cada push, o **GitHub Actions** compila o projeto e roda todos os testes.

## Configuração

```yaml
carrinho:
  abandono:
    minutos: 2           # tempo sem alteração para considerar abandonado
    intervalo-ms: 30000  # de quanto em quanto tempo o job roda

jwt:
  expiracao-segundos: 3600
```

## Como rodar

**Desenvolvimento** (banco e Prometheus no Docker, API pelo IntelliJ ou pelo Maven):

```bash
docker compose up -d postgres prometheus
./mvnw spring-boot:run
```

**Duas instâncias no Docker** (portas 8081 e 8082):

```bash
./mvnw clean package -DskipTests
docker compose up -d --build
```

**Usando a API:**

```bash
# 1. login (guarda o token)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"rose","senha":"123"}' | jq -r .token)

# 2. criar carrinho
curl -X POST http://localhost:8080/carrinhos -H "Authorization: Bearer $TOKEN"

# 3. adicionar produto
curl -X POST http://localhost:8080/carrinhos/1/itens \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"produto":"Fone Bluetooth","quantidade":1,"precoUnitario":199.90}'

# 4. finalizar
curl -X POST http://localhost:8080/carrinhos/1/finalizar -H "Authorization: Bearer $TOKEN"
```

## Próximos passos

- Marcar como abandonado e confirmar a transação antes de enviar o lembrete, para não segurar a
  trava durante o envio (padrão Outbox ou fila com Kafka).
- Corrigir o N+1 do job (uma consulta de itens por carrinho).
- Documentação da API com Swagger/OpenAPI.
- Resiliência no envio do lembrete com Resilience4j (retry, circuit breaker e timeout).
- Migrações do banco com Flyway, no lugar do `ddl-auto: update`.
- Dashboards no Grafana e envio dos alertas pelo Alertmanager.