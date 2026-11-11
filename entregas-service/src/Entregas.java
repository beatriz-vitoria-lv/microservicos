import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Serviço independente de entregas. Mantém apenas seus próprios registros. */
public class Entregas {
    private static final ConcurrentHashMap<Integer, String> entregas = new ConcurrentHashMap<>();
    private static final AtomicInteger sequencia = new AtomicInteger(0);
    private static final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final String PEDIDOS_URL = System.getenv().getOrDefault(
            "PEDIDOS_URL", "http://localhost:8082");

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 8083), 0);
        servidor.createContext("/", Entregas::rotear);
        servidor.start();
        System.out.println("Entregas em http://localhost:8083");
    }

    private static void rotear(HttpExchange exchange) throws IOException {
        String caminho = exchange.getRequestURI().getPath();
        if (caminho.equals("/entregas")) {
            if (!exchange.getRequestMethod().equals("POST")) {
                metodoNaoPermitido(exchange, "POST");
                return;
            }
            criar(exchange);
            return;
        }
        if (caminho.startsWith("/entregas/")) {
            if (!exchange.getRequestMethod().equals("GET")) {
                metodoNaoPermitido(exchange, "GET");
                return;
            }
            consultar(exchange, caminho.substring("/entregas/".length()));
            return;
        }
        responder(exchange, 404, "{\"erro\":\"Rota inexistente\"}");
    }

    private static void criar(HttpExchange exchange) throws IOException {
        byte[] corpo = exchange.getRequestBody().readNBytes(65);
        if (corpo.length > 64) {
            responder(exchange, 413, "{\"erro\":\"Corpo muito grande\"}");
            return;
        }

        int pedidoId;
        try {
            pedidoId = Integer.parseInt(new String(corpo, StandardCharsets.UTF_8).trim());
            if (pedidoId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            responder(exchange, 400, "{\"erro\":\"Envie um ID inteiro positivo no corpo\"}");
            return;
        }

        HttpRequest requisicao;
        try {
            requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(PEDIDOS_URL + "/pedidos/" + pedidoId))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
        } catch (IllegalArgumentException ex) {
            responder(exchange, 503, "{\"erro\":\"Endereco do servico de pedidos invalido\"}");
            return;
        }

        try {
            HttpResponse<Void> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.discarding());
            if (resposta.statusCode() == 404) {
                responder(exchange, 422, "{\"erro\":\"Pedido informado nao existe\"}");
                return;
            }
            if (resposta.statusCode() != 200) {
                responder(exchange, 503, "{\"erro\":\"Servico de pedidos indisponivel\"}");
                return;
            }

            int id = sequencia.incrementAndGet();
            String entrega = "{\"id\":" + id + ",\"pedidoId\":" + pedidoId
                    + ",\"status\":\"AGUARDANDO_ENVIO\"}";
            entregas.put(id, entrega);
            exchange.getResponseHeaders().set("Location", "/entregas/" + id);
            responder(exchange, 201, entrega);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            responder(exchange, 503, "{\"erro\":\"Consulta ao servico de pedidos interrompida\"}");
        } catch (IOException ex) {
            responder(exchange, 503, "{\"erro\":\"Servico de pedidos indisponivel\"}");
        }
    }

    private static void consultar(HttpExchange exchange, String textoId) throws IOException {
        final int id;
        try {
            id = Integer.parseInt(textoId);
            if (id <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            responder(exchange, 400, "{\"erro\":\"ID deve ser inteiro positivo\"}");
            return;
        }
        String entrega = entregas.get(id);
        if (entrega == null) {
            responder(exchange, 404, "{\"erro\":\"Entrega inexistente\"}");
            return;
        }
        responder(exchange, 200, entrega);
    }

    private static void metodoNaoPermitido(HttpExchange exchange, String permitido) throws IOException {
        exchange.getResponseHeaders().set("Allow", permitido);
        responder(exchange, 405, "{\"erro\":\"Metodo nao permitido\"}");
    }

    private static void responder(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
        System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " -> " + status);
    }
}
