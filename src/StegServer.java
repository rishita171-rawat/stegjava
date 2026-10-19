package src;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class StegServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9090), 0);
        server.createContext("/", StegServer::serveFile);
        server.createContext("/encode", StegServer::encode);
        server.start();
        System.out.println("Running. Open http://localhost:9090");
    }

    // Serves index.html, style.css and app.js from the stegjava folder.
    private static void serveFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        Path root = Path.of("stegjava").toAbsolutePath().normalize();
        Path file = root.resolve(path.substring(1)).normalize();

        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }

        String name = file.getFileName().toString();
        String type = name.endsWith(".html") ? "text/html; charset=UTF-8"
                    : name.endsWith(".css")  ? "text/css"
                    : name.endsWith(".js")   ? "text/javascript"
                    : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(file));
    }

    // Receives image + message, calls your existing core, returns a PNG.
    private static void encode(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestMethod().equals("POST")) {
                send(ex, 405, "text/plain", "Use POST".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = new HashMap<>();
            for (String pair : body.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    form.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                             URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }

            String message = form.get("message");
            String imageData = form.get("image");

            if (imageData == null || imageData.isEmpty()) {
                throw new IllegalArgumentException("Please upload an image.");
            }
            if (message == null || message.isEmpty()) {
                throw new IllegalArgumentException("Please enter a secret message.");
            }

            BufferedImage image = ImageIO.read(
                    new ByteArrayInputStream(Base64.getDecoder().decode(imageData)));
            if (image == null) {
                throw new IllegalArgumentException("Invalid image file.");
            }

            BufferedImage encoded = LSBSteganography.encode(image, message);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(encoded, "png", out);
            send(ex, 200, "image/png", out.toByteArray());

        } catch (IllegalArgumentException e) {
            send(ex, 400, "text/plain; charset=UTF-8", e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            send(ex, 500, "text/plain; charset=UTF-8", ("Server error: " + e).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void send(HttpExchange ex, int status, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }
}