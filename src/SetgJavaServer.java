
package src;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/SetgJavaServer")
@MultipartConfig(
    maxFileSize = 10 * 1024 * 1024,
    maxRequestSize = 11 * 1024 * 1024
)
public class SetgJavaServer extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        try {
            if ("encode".equals(action)) {
                encodeImage(request, response);
            } else if ("decode".equals(action)) {
                decodeImage(request, response);
            } else {
                sendError(response, 400, "Invalid action.");
            }
        } catch (IllegalArgumentException e) {
            sendError(response, 400, e.getMessage());
        }
    }

    // Hide a message inside an image.
    private void encodeImage(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {

        Part imagePart = request.getPart("image");
        String message = request.getParameter("message");

        if (imagePart == null || imagePart.getSize() == 0) {
            throw new IllegalArgumentException(
                "Please upload an image."
            );
        }

        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException(
                "Please enter a secret message."
            );
        }

        BufferedImage image;

        try (InputStream input = imagePart.getInputStream()) {
            image = ImageIO.read(input);
        }

        if (image == null) {
            throw new IllegalArgumentException(
                "Invalid image file."
            );
        }

        // Call your existing steganography class.
        BufferedImage encoded =
            LSBSteganography.encode(image, message);

        response.setContentType("image/png");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"encoded-image.png\""
        );

        ImageIO.write(
            encoded,
            "png",
            response.getOutputStream()
        );
    }

    // Extract a hidden message from an image.
    private void decodeImage(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {

        Part imagePart = request.getPart("image");

        if (imagePart == null || imagePart.getSize() == 0) {
            throw new IllegalArgumentException(
                "Please upload an encoded image."
            );
        }

        BufferedImage image;

        try (InputStream input = imagePart.getInputStream()) {
            image = ImageIO.read(input);
        }

        if (image == null) {
            throw new IllegalArgumentException(
                "Invalid image file."
            );
        }

        // Call your existing decoding method.
        String message = LSBSteganography.decode(image);

        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
