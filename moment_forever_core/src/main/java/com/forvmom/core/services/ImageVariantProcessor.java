package com.forvmom.core.services;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImageVariantProcessor {

    private ImageVariantProcessor() {
    }

    public static class RenderedVariant {
        private final byte[] bytes;
        private final int width;
        private final int height;
        private final String format;
        private final String mimeType;

        public RenderedVariant(byte[] bytes, int width, int height, String format, String mimeType) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
            this.format = format;
            this.mimeType = mimeType;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getFormat() {
            return format;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    public static BufferedImage decode(byte[] data) {
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(data));
        } catch (IOException e) {
            return null;
        }
    }

    public static RenderedVariant resize(BufferedImage source, int maxWidth, String preferredFormat) throws IOException {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int targetWidth = Math.min(srcWidth, maxWidth);
        int targetHeight = (int) Math.round((double) srcHeight * targetWidth / srcWidth);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        String format = normalizeFormat(preferredFormat);
        byte[] bytes = encode(resized, format);
        if (bytes == null) {
            format = "jpg";
            bytes = encode(resized, format);
        }
        if (bytes == null) {
            throw new IOException("Could not encode resized image variant");
        }
        return new RenderedVariant(bytes, targetWidth, targetHeight, format, mimeTypeFor(format));
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean ok = ImageIO.write(image, format, baos);
        if (!ok) {
            return null;
        }
        return baos.toByteArray();
    }

    private static String normalizeFormat(String format) {
        if (format == null) {
            return "jpg";
        }
        String f = format.toLowerCase();
        if (f.equals("jpeg")) {
            return "jpg";
        }
        if (f.equals("png") || f.equals("jpg")) {
            return f;
        }
        return "jpg";
    }

    private static String mimeTypeFor(String format) {
        return "png".equals(format) ? "image/png" : "image/jpeg";
    }
}

