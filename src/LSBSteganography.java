package src;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

public class LSBSteganography {

    // Hide the message in the image.
    public static BufferedImage encode(
            BufferedImage image, String message) {

        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        // Four bytes store the message length.
        long requiredBits = 32L + (long) data.length * 8;
        long capacity = (long) image.getWidth()
                * image.getHeight() * 3;

        if (requiredBits > capacity) {
            throw new IllegalArgumentException(
                    "Message too long for this image!");
        }

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                result.setRGB(x, y, image.getRGB(x, y));
            }
        }

        // Store message length first.
        for (int i = 0; i < 32; i++) {
            int bit = (data.length >>> (31 - i)) & 1;
            writeBit(result, i, bit);
        }

        // Store each message byte.
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < 8; j++) {
                int bit = ((data[i] & 0xFF) >>> (7 - j)) & 1;
                long position = 32L + (long) i * 8 + j;
                writeBit(result, position, bit);
            }
        }

        return result;
    }

    // Extract the message from the image.
    public static String decode(BufferedImage image) {

        long capacity = (long) image.getWidth()
                * image.getHeight() * 3;

        if (capacity < 32) {
            throw new IllegalArgumentException("Image is too small.");
        }

        int length = 0;

        for (int i = 0; i < 32; i++) {
            length = (length << 1) | readBit(image, i);
        }

        long maxBytes = (capacity - 32) / 8;

        if (length < 0 || length > maxBytes) {
            throw new IllegalArgumentException(
                    "No valid hidden message found.");
        }

        byte[] data = new byte[length];

        for (int i = 0; i < length; i++) {
            int value = 0;

            for (int j = 0; j < 8; j++) {
                long position = 32L + (long) i * 8 + j;
                value = (value << 1) | readBit(image, position);
            }

            data[i] = (byte) value;
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    // Write one bit into an RGB channel.
    private static void writeBit(
            BufferedImage image, long position, int bit) {

        int width = image.getWidth();
        int x = (int) ((position / 3) % width);
        int y = (int) ((position / 3) / width);
        int channel = (int) (position % 3);

        int rgb = image.getRGB(x, y);
        int shift = channel == 0 ? 16 : channel == 1 ? 8 : 0;

        int color = (rgb >>> shift) & 0xFF;
        color = (color & 0xFE) | bit;

        int updated = (rgb & ~(0xFF << shift))
                | (color << shift);

        image.setRGB(x, y, updated);
    }

    // Read one bit from an RGB channel.
    private static int readBit(
            BufferedImage image, long position) {

        int width = image.getWidth();
        int x = (int) ((position / 3) % width);
        int y = (int) ((position / 3) / width);
        int channel = (int) (position % 3);

        int rgb = image.getRGB(x, y);
        int shift = channel == 0 ? 16 : channel == 1 ? 8 : 0;

        return (rgb >>> shift) & 1;
    }
}
