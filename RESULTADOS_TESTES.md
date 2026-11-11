# Resultados de testes — Serviço de Entregas

Execução local de verificação em **JDK 21**, com os três serviços compilados por `javac --add-modules jdk.httpserver` e executados em processos separados.

| Teste | Resultado observado | Status |
|---|---|---|
| Pedido existente → criar entrega | `{"id":1,"pedidoId":1,"status":"AGUARDANDO_ENVIO"}` | 201 — OK |
| Consultar entrega criada | Mesmo JSON da criação | 200 — OK |
| Pedido inexistente | Pedido informado não existe | 422 — OK |
| Corpo não numérico | Erro de entrada | 400 — OK |
| ID zero | Erro de entrada | 400 — OK |
| ID inválido no GET | ID deve ser inteiro positivo | 400 — OK |
| Entrega inexistente | Entrega inexistente | 404 — OK |
| Método incorreto | Método não permitido; contrato define Allow: POST | 405 — OK |
| Rota desconhecida | Rota inexistente | 404 — OK |
| Produtos parado, Pedidos ativo, pedido já criado | Segunda entrega criada | 201 — OK |
| Pedidos parado, POST de entrega | Serviço de pedidos indisponível | 503 — OK |
| Pedidos parado, GET de entrega salva | Registro local retornado | 200 — OK |
| Reiniciar Entregas e consultar ID anterior | Entrega inexistente | 404 — OK |

## Evidências
Os status HTTP e corpos acima foram observados na execução local. Este arquivo é um registro textual; não substitui capturas de tela do IntelliJ. Para a entrega no Teams, podem ser anexadas capturas do cliente HTTP e dos consoles durante os testes de parada/reinício.
