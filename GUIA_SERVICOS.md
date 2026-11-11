# Guia de execução — IntelliJ IDEA

## Requisitos
- IntelliJ IDEA Community ou Ultimate.
- JDK 17 ou superior (JDK 21 recomendado).
- Portas 8081, 8082 e 8083 livres.
- Não é necessário Spring Boot, Maven, Gradle ou banco de dados.

## Iniciar os serviços
Cada pasta é um projeto independente. Abra cada uma com **File > Open** e escolha **New Window** para manter os processos ativos simultaneamente. Em cada janela, selecione o JDK em **File > Project Structure > Project** e marque `src` como Sources Root, se necessário.

1. Em `produtos-service`, execute `src/Produtos.java` pelo método `Produtos.main()`. Serviço na porta 8081.
2. Em `pedidos-service`, execute `src/Pedidos.java` pelo método `Pedidos.main()`. Serviço na porta 8082.
3. Em `entregas-service`, execute `src/Entregas.java` pelo método `Entregas.main()`. Serviço na porta 8083.

Mantenha as três janelas em execução durante os testes normais.

## Executar testes HTTP
Abra os arquivos `testes.http` de cada serviço no IntelliJ e execute as requisições individualmente:
- `pedidos-service/testes.http`: consultas de Produtos e criação/consulta de Pedidos.
- `entregas-service/testes.http`: criação e consulta de Entregas e cenários de falha.

O POST de Pedidos recebe o ID do produto como texto simples (`text/plain`). O POST de Entregas recebe o ID do pedido, também como texto simples.

## Testes de indisponibilidade
Alguns cenários exigem interromper manualmente um serviço:
- Pare Produtos para verificar que Pedidos ainda consulta pedidos já salvos, mas não cria novos.
- Pare Pedidos para verificar que Entregas retorna 503 ao criar uma entrega, mas continua consultando entregas já registradas.
- Reinicie Entregas e consulte um ID antigo: a resposta esperada é 404, pois os registros são mantidos somente em memória.

Consulte o README.md para o contrato HTTP completo e RESULTADOS_TESTES.md para os resultados observados.

## Execução pelo terminal
Abra três terminais, cada um na pasta `src` do respectivo serviço, e execute:
```bash
java --add-modules jdk.httpserver Produtos.java
java --add-modules jdk.httpserver Pedidos.java
java --add-modules jdk.httpserver Entregas.java
```
