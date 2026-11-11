# Microsserviços Java — Produtos, Pedidos e Entregas

## Integrante
- **Nome:** Beatriz Vitória Lourenço Vitoriano

## Visão geral
Projeto didático com três serviços Java independentes, executados em processos separados e integrados por HTTP:
- **Produtos** (`produtos-service`): catálogo fixo, porta `8081`.
- **Pedidos** (`pedidos-service`): cria pedidos consultando Produtos, porta `8082`.
- **Entregas** (`entregas-service`): cria entregas consultando Pedidos, porta `8083`.

Cada serviço mantém seus próprios dados em memória. Reiniciar um processo apaga seus registros. Não há banco de dados.

## Requisitos
- IntelliJ IDEA Community ou Ultimate.
- JDK 17 ou superior (JDK 21 recomendado; inclui `jdk.httpserver`).
- Portas locais `8081`, `8082` e `8083` livres.
- Não é necessário Spring Boot, Maven, Gradle ou dependências externas.

## Arquitetura
```mermaid
flowchart LR
    Cliente[Cliente HTTP / testes.http] -->|GET /produtos/{id}| P[Produtos :8081]
    Cliente -->|POST e GET /pedidos| O[Pedidos :8082]
    Cliente -->|POST e GET /entregas| E[Entregas :8083]
    O -->|GET /produtos/{id}| P
    E -->|GET /pedidos/{id}| O
    P --- MP[(Memória local Produtos)]
    O --- MO[(Memória local Pedidos)]
    E --- ME[(Memória local Entregas)]
```

O cliente envia um ID de pedido para Entregas. Entregas consulta `GET /pedidos/{id}`. Se Pedidos responder `200`, Entregas gera seu próprio ID, salva localmente e responde `201` com `Location`. Se Pedidos responder `404`, responde `422`. Em falhas de rede, timeout ou outro status remoto, responde `503` sem gravar. A consulta de entrega salva usa somente a memória local de Entregas.

## Abrir e executar no IntelliJ
Abra cada pasta como projeto separado com **File > Open**. Para manter os três serviços ativos, abra cada um em uma janela distinta (**New Window**).
1. Extraia o repositório.
2. Abra `produtos-service`; em **File > Project Structure > Project**, selecione um Project SDK JDK 17+ e confirme `src` como Sources Root.
3. Execute `src/Produtos.java` pelo método `Produtos.main()`. Aguarde a porta 8081.
4. Abra `pedidos-service` em outra janela, selecione o JDK e execute `Pedidos.main()`. Aguarde a porta 8082.
5. Abra `entregas-service` em outra janela, selecione o JDK e execute `Entregas.main()`. Aguarde a porta 8083.
6. Mantenha os três processos ativos durante os testes normais.

### Execução pelo terminal
Em terminais separados, a partir da pasta `src` de cada serviço:
```bash
java --add-modules jdk.httpserver Produtos.java
java --add-modules jdk.httpserver Pedidos.java
java --add-modules jdk.httpserver Entregas.java
```

### Endereços
- Produtos: http://localhost:8081/produtos/1
- Pedidos: http://localhost:8082/pedidos/1
- Entregas: http://localhost:8083/entregas/1

## Contrato de Entregas
| Método e rota | Comportamento |
|---|---|
| `POST /entregas` | Recebe ID inteiro positivo em `text/plain`, consulta Pedidos e cria entrega |
| `GET /entregas/{id}` | Consulta somente a memória local de Entregas |
| Método incorreto | `405` com cabeçalho `Allow` |
| Rota desconhecida | `404` |

| Situação do POST | HTTP |
|---|---:|
| Pedido encontrado | `201 Created` |
| Corpo inválido ou ID não positivo | `400 Bad Request` |
| Pedido não existe (Pedidos responde 404) | `422 Unprocessable Entity` |
| Timeout, falha de rede ou outro status remoto | `503 Service Unavailable` |

Exemplo:
```json
{"id":1,"pedidoId":1,"status":"AGUARDANDO_ENVIO"}
```
O ID da entrega é independente do ID do pedido. Mais de uma entrega para o mesmo pedido é permitido.

## Testes
Abra `entregas-service/testes.http` no IntelliJ e execute as requisições individualmente na ordem indicada. Os testes que exigem parar um serviço devem ser executados manualmente no momento indicado. `pedidos-service/testes.http` contém testes de Produtos e Pedidos.

### Matriz de testes — Entregas
| ID | Cenário | Esperado | Obtido | Evidência |
|---|---|---|---|---|
| E01 | Criar para pedido existente | 201 + Location + JSON | 201 — OK | POST retornou JSON de entrega |
| E02 | Consultar entrega criada | 200 | 200 — OK | GET retornou JSON salvo |
| E03 | Pedido inexistente | 422 | 422 — OK | POST retornou erro de pedido |
| E04 | Corpo não numérico | 400 | 400 — OK | POST retornou erro de entrada |
| E05 | ID zero/negativo | 400 | 400 — OK | POST com 0 retornou erro |
| E06 | ID inválido no GET | 400 | 400 — OK | GET /entregas/abc |
| E07 | Entrega inexistente | 404 | 404 — OK | GET /entregas/999 |
| E08 | Método incorreto | 405 + Allow: POST | 405 — OK | GET /entregas retornou 405 |
| E09 | Rota desconhecida | 404 | 404 — OK | GET /rota-inexistente |
| E10 | Pedidos desligado, POST | 503 | 503 — OK | Serviço remoto indisponível |
| E11 | Pedidos desligado, GET local | 200 | 200 — OK | Registro local consultado |
| E12 | Produtos desligado, pedido já criado | 201 | 201 — OK | Segunda entrega criada |
| E13 | Reiniciar Entregas e consultar ID antigo | 404 | 404 — OK | Memória reiniciada |

Os 13 cenários foram executados localmente em JDK 21 e os status coincidiram com os esperados. `RESULTADOS_TESTES.md` registra os resultados. Para a entrega escolar, recomenda-se anexar capturas de tela do IntelliJ/cliente HTTP e dos consoles, especialmente nos testes de parada e reinício.

## Organização
```text
.
├── README.md
├── RESULTADOS_TESTES.md
├── GUIA_SERVICOS.md
├── .gitignore
├── produtos-service/
│   ├── .gitignore
│   └── src/Produtos.java
├── pedidos-service/
│   ├── .gitignore
│   ├── src/Pedidos.java
│   └── testes.http
└── entregas-service/
    ├── .gitignore
    ├── src/Entregas.java
    └── testes.http
```

