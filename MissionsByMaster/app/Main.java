import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {

  // ─── Логика поиска (не изменилась) ───
  public static List<Integer> findIndex(int[] arr, int x) {
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == x) {
        result.add(i);
      }
    }
    return result;
  }

  // ─── Массив теперь НЕ final — можно обновлять ───
  private static int[] ARR;
  private static final Random RANDOM = new Random();

  // генерация нового массива
  private static synchronized void regenerateArray() {
    ARR = new int[100];
    for (int i = 0; i < ARR.length; i++) {
      ARR[i] = RANDOM.nextInt(100);
    }
    System.out.println("🔄 Массив обновлён: " + Arrays.toString(ARR));
  }

  public static void main(String[] args) throws IOException {
    regenerateArray(); // первичная генерация

    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    // / → index.html, style.css
    server.createContext("/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      if (path.equals("/") || path.equals("/index.html")) {
        serveFile(exchange, "static/index.html", "text/html; charset=UTF-8");
      } else if (path.equals("/style.css")) {
        serveFile(exchange, "static/style.css", "text/css; charset=UTF-8");
      } else {
        sendText(exchange, 404, "Not Found");
      }
    });

    // /api/find?x=42
    server.createContext("/api/find", Main::handleFind);

    // /api/regenerate → пересоздаёт массив
    server.createContext("/api/regenerate", exchange -> {
      regenerateArray();
      sendJson(exchange, "{\"ok\":true,\"size\":" + ARR.length + "}");
    });

    server.setExecutor(null);
    server.start();

    System.out.println("================================================");
    System.out.println("  ✅ Сервер запущен:  http://localhost:8080");
    System.out.println("  Остановить:        Ctrl + C");
    System.out.println("================================================");
  }

  private static void handleFind(HttpExchange exchange) throws IOException {
    try {
      String query = exchange.getRequestURI().getQuery();
      int x = -1;
      if (query != null) {
        for (String pair : query.split("&")) {
          String[] kv = pair.split("=");
          if (kv.length == 2 && kv[0].equals("x")) {
            x = Integer.parseInt(URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
          }
        }
      }

      if (x < 0) {
        sendJson(exchange, "{\"error\":\"invalid number\"}");
        return;
      }

      List<Integer> indices = findIndex(ARR, x);
      String json = indices.isEmpty()
          ? "{\"found\":false}"
          : "{\"found\":true,\"indices\":" + indices + "}";
      sendJson(exchange, json);

    } catch (NumberFormatException e) {
      sendJson(exchange, "{\"error\":\"invalid number\"}");
    }
  }

  private static void serveFile(HttpExchange exchange, String filePath, String contentType) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) {
      sendText(exchange, 404, "File not found: " + filePath);
      return;
    }
    byte[] bytes = Files.readAllBytes(file.toPath());
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private static void sendJson(HttpExchange exchange, String json) throws IOException {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }
}