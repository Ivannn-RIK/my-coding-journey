import com.sun.net.httpserver.HttpServer; // всторенный http сервер
import com.sun.net.httpserver.HttpExchange; // HTTP-обмен
import java.io.File; // для работы с файлами в файловой системе
import java.io.IOException; // ошибки вывода и ввода
import java.net.InetSocketAddress; // создание ip
import java.net.URLDecoder; // Для декодирования URL-строк
import java.nio.charset.StandardCharsets; // UTF-8
import java.nio.file.Files; // Читает index.html и style.css одной строкой

public class Main {
  // создание HTTP сервера
  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    // подключение index.html
    server.createContext("/", exchange -> {
      File file = new File("app/templates/index.html");
      // Проверка на наличие index.html
      if (!file.exists()) {
        send(exchange, 404, "text/plain", "index.html не найден");
        return;
      }
      // Сложная схема
      // * readAllBytes читает весь файл в массив байт одной строкой.
      // file.toPath() конвертирует File в java.nio.file.Path,
      // потому что Files работает именно с Path. */
      byte[] bytes = Files.readAllBytes(file.toPath());
      send(exchange, 200, "text/html; charset=utf-8", bytes);
    });

    // Подключение style.css из папки static
    server.createContext("/static/", exchange -> {
      String path = exchange.getRequestURI().getPath(); // /static/style.css
      // Отрезаем префикс "/static/" и подставляем реальный путь "app/static/"
      // было: new File("." + path) -> ./static/style.css (ищет в корне проекта)
      // стало: new File("app/static/" + relative) -> app/static/style.css
      String relative = path.substring("/static/".length()); // style.css
      File file = new File("app/static/" + relative);

      // Проверка на наличие css
      if (!file.exists()) {
        send(exchange, 404, "text/plain", "Не найдено");
        return;
      }
      byte[] bytes = Files.readAllBytes(file.toPath());
      send(exchange, 200, "text/css; charset=utf-8", bytes);
    });

    // Логика реверса
    server.createContext("/reverse", exchange -> {
      String query = exchange.getRequestURI().getQuery(); // num=12345
      String num = "";
      if (query != null && query.startsWith("num=")) {
        num = URLDecoder.decode(query.substring(4), StandardCharsets.UTF_8);
      }
      String numtext = String.valueOf(num);
      StringBuilder reversed = new StringBuilder();
      for (int i = numtext.length() - 1; i >= 0; i--) {
        reversed.append(numtext.charAt(i));
      }

      // Защита от поломки
      String json = "{\"reversed\":\"" + escape(reversed.toString()) + "\"}";
      send(exchange, 200, "application/json; charset=utf-8",
          json.getBytes(StandardCharsets.UTF_8));
    });

    // Запуск сервера
    server.start();
    System.out.println("Открой http://localhost:8080");
  }

  // Универсальный ответ
  private static void send(HttpExchange ex, int code, String contentType, byte[] body) throws IOException {
    ex.getResponseHeaders().add("Content-Type", contentType);
    ex.sendResponseHeaders(code, body.length);
    ex.getResponseBody().write(body);
    ex.close();
  }

  private static void send(HttpExchange ex, int code, String contentType, String body) throws IOException {
    send(ex, code, contentType, body.getBytes(StandardCharsets.UTF_8));
  }

  // Простое экранирование для JSON
  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}

// public class Main {
// public static void main(String[] args){
// while (true) {
// System.out.println("Введи число, которое хочешь реверснуть!");
// Scanner scanner = new Scanner(System.in);
// int num = scanner.nextInt();

// String numtext = String.valueOf(num);

// System.out.print("Реверснутое число: ");
// for (int i = numtext.length() - 1; i >= 0; i--) {
// System.out.print(numtext.charAt(i));
// ;
// }

// }
// }
// }