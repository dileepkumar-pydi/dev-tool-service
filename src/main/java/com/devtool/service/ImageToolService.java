package com.devtool.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.devtool.dto.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;

@Service
public class ImageToolService {
    public static final List<String> OPERATIONS = List.of("compress", "resize", "crop", "convert-to-jpg",
            "convert-from-jpg", "photo-editor", "upscale", "remove-background", "watermark", "meme", "rotate", "html-to-image", "blur");
    private static final long MAX_PIXELS = 12_000_000;
    private static final long MAX_BATCH_PIXELS = 40_000_000;
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private record Source(BufferedImage image, String format) {}

    public ImageBatchResponse process(String operation, List<MultipartFile> files, Map<String, String> options, MultipartFile logo) {
        if (!OPERATIONS.contains(operation)) throw new IllegalArgumentException("Unknown image operation.");
        if (files == null || files.isEmpty() || files.size() > 20) throw new IllegalArgumentException("Choose between 1 and 20 files.");
        if (files.stream().mapToLong(MultipartFile::getSize).sum() > 80L * 1024 * 1024) throw new IllegalArgumentException("Choose at most 80 MB per batch.");
        if (operation.equals("html-to-image")) return renderHtml(files, options);
        try {
            BufferedImage stamp = logo == null || logo.isEmpty() ? null : read(logo).image;
            List<ImageFileResultResponse> results = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            boolean animation = operation.equals("convert-from-jpg") && choice(options, "format", "png", "png", "gif").equals("gif") && files.size() > 1;
            List<BufferedImage> frames = new ArrayList<>();
            long pixels = 0;
            long outputBytes = 0;
            long outputPixels = 0;
            for (MultipartFile file : files) {
                Source source = read(file);
                BufferedImage image = source.image;
                pixels += (long) image.getWidth() * image.getHeight();
                if (pixels > MAX_BATCH_PIXELS) throw new IllegalArgumentException("Batch exceeds 40 million decoded pixels. Process fewer images.");
                if (operation.equals("convert-from-jpg") && !source.format.equalsIgnoreCase("JPEG")) throw new IllegalArgumentException("Convert from JPG accepts JPEG files only.");
                if (animation) { frames.add(image); continue; }
                String format = "png";
                BufferedImage output;
                switch (operation) {
                    case "compress" -> {
                        format = source.format.equalsIgnoreCase("JPEG") ? "jpg" : "png";
                        output = image;
                    }
                    case "resize" -> {
                        int width, height;
                        if (choice(options, "mode", "pixels", "pixels", "percent").equals("percent")) {
                            double percent = number(options, "percent", 50, 1, 400) / 100;
                            width = Math.max(1, (int) Math.round(image.getWidth() * percent));
                            height = Math.max(1, (int) Math.round(image.getHeight() * percent));
                        } else {
                            width = integer(options, "width", 800, 1, 12000);
                            height = integer(options, "height", 600, 1, 12000);
                            if (bool(options, "keepAspect", true)) {
                                double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
                                width = Math.max(1, (int) Math.round(image.getWidth() * scale));
                                height = Math.max(1, (int) Math.round(image.getHeight() * scale));
                            }
                        }
                        output = resize(image, width, height);
                    }
                    case "upscale" -> {
                        int scale = integer(options, "scale", 2, 2, 4);
                        if (scale != 2 && scale != 4) throw new IllegalArgumentException("Choose a 2x or 4x scale.");
                        output = resize(image, image.getWidth() * scale, image.getHeight() * scale);
                    }
                    case "crop" -> output = region(image, options, false);
                    case "blur" -> output = region(image, options, true);
                    case "rotate" -> output = rotate(image, integer(options, "angle", 90, 0, 270), choice(options, "flip", "none", "none", "horizontal", "vertical"));
                    case "convert-to-jpg" -> { output = image; format = "jpg"; }
                    case "convert-from-jpg" -> { output = image; format = choice(options, "format", "png", "png", "gif"); }
                    case "remove-background" -> output = removeBackground(image, color(options, "background", "#ffffff"), integer(options, "tolerance", 30, 0, 255));
                    case "photo-editor" -> output = edit(image, options);
                    case "watermark" -> output = watermark(image, options, stamp);
                    case "meme" -> output = meme(image, options);
                    default -> throw new IllegalArgumentException("Unknown image operation.");
                }
                outputPixels += (long) output.getWidth() * output.getHeight();
                if (outputPixels > MAX_BATCH_PIXELS) throw new IllegalArgumentException("Output exceeds 40 million pixels. Process fewer images.");
                byte[] bytes = encode(output, format, (float) number(options, "quality", 80, 1, 100) / 100,
                        color(options, "background", "#ffffff"));
                if (operation.equals("compress") && bytes.length >= file.getSize()) {
                    warnings.add(file.getOriginalFilename() + ": already optimized; original file retained.");
                    // Preserve the original format, including GIF/BMP/TIFF, when re-encoding cannot reduce it.
                    results.add(result(stem(file) + "-original." + extension(source.format), mime(source.format), file.getBytes(), file.getSize(), image));
                } else {
                    results.add(result(stem(file) + "-" + operation + "." + format, mime(format), bytes, file.getSize(), output));
                }
                outputBytes += results.get(results.size() - 1).size();
                if (outputBytes > 60L * 1024 * 1024) throw new IllegalArgumentException("Output batch is too large. Process fewer images.");
            }
            if (animation) {
                byte[] bytes = gif(frames, integer(options, "delay", 500, 20, 10000), bool(options, "loop", true));
                if (bytes.length > 60L * 1024 * 1024) throw new IllegalArgumentException("Animation is too large. Use fewer or smaller frames.");
                results.add(result("animation.gif", "image/gif", bytes, files.stream().mapToLong(MultipartFile::getSize).sum(), frames.get(0)));
            }
            return new ImageBatchResponse(operation, results, warnings);
        } catch (IOException error) {
            throw new IllegalArgumentException("Unable to process these images. Check the format and try again.", error);
        }
    }

    private Source read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Each image must be nonempty and at most 20 MB.");
        try (ImageInputStream input = new MemoryCacheImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Unsupported image. Use JPG, PNG, static GIF, BMP, or TIFF.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                dimensions(reader.getWidth(0), reader.getHeight(0));
                if (reader.getNumImages(true) != 1) throw new IllegalArgumentException("Animated or multipage inputs are not supported. Upload a single-frame image.");
                return new Source(reader.read(0), reader.getFormatName());
            } finally { reader.dispose(); }
        }
    }

    private static void dimensions(int width, int height) {
        if (width < 1 || height < 1 || width > 12000 || height > 12000 || (long) width * height > MAX_PIXELS)
            throw new IllegalArgumentException("Each input and output must be at most 12 million pixels and 12,000 pixels per side.");
    }

    private BufferedImage canvas(int width, int height) {
        dimensions(width, height);
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    private BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage out = canvas(width, height);
        Graphics2D g = graphics(out);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private BufferedImage region(BufferedImage source, Map<String, String> options, boolean blur) {
        int x = integer(options, "x", 0, 0, source.getWidth() - 1);
        int y = integer(options, "y", 0, 0, source.getHeight() - 1);
        int width = integer(options, "width", source.getWidth() - x, 1, source.getWidth() - x);
        int height = integer(options, "height", source.getHeight() - y, 1, source.getHeight() - y);
        if (!blur) {
            BufferedImage out = canvas(width, height);
            Graphics2D g = graphics(out);
            g.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
            g.dispose();
            return out;
        }
        BufferedImage out = resize(source, source.getWidth(), source.getHeight());
        int block = integer(options, "strength", 16, 4, 100);
        // Pixelation hides the selected region without leaking neighboring pixels into it.
        for (int row = y; row < y + height; row += block) for (int col = x; col < x + width; col += block) {
            int bw = Math.min(block, x + width - col), bh = Math.min(block, y + height - row);
            long a = 0, r = 0, g = 0, b = 0;
            for (int dy = 0; dy < bh; dy++) for (int dx = 0; dx < bw; dx++) {
                int pixel = source.getRGB(col + dx, row + dy);
                a += pixel >>> 24; r += pixel >> 16 & 255; g += pixel >> 8 & 255; b += pixel & 255;
            }
            int count = bw * bh;
            int rgb = (int) (a / count) << 24 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
            for (int dy = 0; dy < bh; dy++) for (int dx = 0; dx < bw; dx++) out.setRGB(col + dx, row + dy, rgb);
        }
        return out;
    }

    private BufferedImage rotate(BufferedImage source, int angle, String flip) {
        if (angle % 90 != 0) throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270 degrees.");
        int sw = source.getWidth(), sh = source.getHeight();
        BufferedImage out = canvas(angle % 180 == 0 ? sw : sh, angle % 180 == 0 ? sh : sw);
        Graphics2D g = graphics(out);
        if (angle == 90) { g.translate(sh, 0); g.rotate(Math.PI / 2); }
        if (angle == 180) { g.translate(sw, sh); g.rotate(Math.PI); }
        if (angle == 270) { g.translate(0, sw); g.rotate(-Math.PI / 2); }
        if (flip.equals("horizontal")) { g.translate(sw, 0); g.scale(-1, 1); }
        if (flip.equals("vertical")) { g.translate(0, sh); g.scale(1, -1); }
        g.drawImage(source, 0, 0, null); g.dispose();
        return out;
    }

    private BufferedImage removeBackground(BufferedImage source, Color background, int tolerance) {
        BufferedImage out = resize(source, source.getWidth(), source.getHeight());
        int width = source.getWidth(), height = source.getHeight();
        // Flood fill only matching colors connected to an edge, retaining enclosed details.
        boolean[] visited = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0, tail = 0;
        for (int x = 0; x < width; x++) { tail = enqueue(source, background, tolerance, x, 0, visited, queue, tail); tail = enqueue(source, background, tolerance, x, height - 1, visited, queue, tail); }
        for (int y = 0; y < height; y++) { tail = enqueue(source, background, tolerance, 0, y, visited, queue, tail); tail = enqueue(source, background, tolerance, width - 1, y, visited, queue, tail); }
        while (head < tail) {
            int index = queue[head++], x = index % width, y = index / width;
            out.setRGB(x, y, source.getRGB(x, y) & 0x00ffffff);
            if (x > 0) tail = enqueue(source, background, tolerance, x - 1, y, visited, queue, tail);
            if (x + 1 < width) tail = enqueue(source, background, tolerance, x + 1, y, visited, queue, tail);
            if (y > 0) tail = enqueue(source, background, tolerance, x, y - 1, visited, queue, tail);
            if (y + 1 < height) tail = enqueue(source, background, tolerance, x, y + 1, visited, queue, tail);
        }
        return out;
    }

    private int enqueue(BufferedImage source, Color background, int tolerance, int x, int y, boolean[] visited, int[] queue, int tail) {
        int index = y * source.getWidth() + x;
        if (visited[index]) return tail;
        visited[index] = true;
        int pixel = source.getRGB(x, y);
        if ((pixel >>> 24) == 0 || Math.max(Math.abs((pixel >> 16 & 255) - background.getRed()),
                Math.max(Math.abs((pixel >> 8 & 255) - background.getGreen()), Math.abs((pixel & 255) - background.getBlue()))) <= tolerance) queue[tail++] = index;
        return tail;
    }

    private BufferedImage edit(BufferedImage source, Map<String, String> options) {
        double brightness = number(options, "brightness", 0, -100, 100) * 2.55;
        double contrast = number(options, "contrast", 100, 0, 200) / 100;
        String filter = choice(options, "filter", "none", "none", "grayscale", "sepia", "invert");
        BufferedImage out = canvas(source.getWidth(), source.getHeight());
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) {
            int pixel = source.getRGB(x, y);
            double r = clamp(((pixel >> 16 & 255) - 128) * contrast + 128 + brightness);
            double g = clamp(((pixel >> 8 & 255) - 128) * contrast + 128 + brightness);
            double b = clamp(((pixel & 255) - 128) * contrast + 128 + brightness);
            if (filter.equals("grayscale")) { double gray = .299 * r + .587 * g + .114 * b; r = gray; g = gray; b = gray; }
            else if (filter.equals("sepia")) { double nr = .393 * r + .769 * g + .189 * b, ng = .349 * r + .686 * g + .168 * b; b = .272 * r + .534 * g + .131 * b; r = nr; g = ng; }
            else if (filter.equals("invert")) { r = 255 - r; g = 255 - g; b = 255 - b; }
            out.setRGB(x, y, pixel & 0xff000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b));
        }
        int border = integer(options, "border", 0, 0, 100);
        if (border > 0) { Graphics2D g = graphics(out); g.setColor(color(options, "color", "#ffffff")); g.setStroke(new BasicStroke(border * 2)); g.drawRect(0, 0, out.getWidth(), out.getHeight()); g.dispose(); }
        if (!options.getOrDefault("text", "").isBlank()) return watermark(out, options, null);
        return out;
    }

    private BufferedImage watermark(BufferedImage source, Map<String, String> options, BufferedImage stamp) {
        String text = text(options, "text");
        if (stamp == null && text.isBlank()) throw new IllegalArgumentException("Enter watermark text or choose a logo image.");
        BufferedImage out = resize(source, source.getWidth(), source.getHeight());
        Graphics2D g = graphics(out);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) number(options, "opacity", 60, 1, 100) / 100));
        int width, height;
        int fontSize = integer(options, "fontSize", 32, 8, 300);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        if (stamp != null) { width = Math.max(1, (int) (source.getWidth() * number(options, "logoScale", 20, 1, 100) / 100)); height = Math.max(1, (int) ((double) width * stamp.getHeight() / stamp.getWidth())); }
        else { width = g.getFontMetrics().stringWidth(text); height = g.getFontMetrics().getHeight(); }
        if (width > out.getWidth() || height > out.getHeight()) { double scale = Math.min((double) out.getWidth() / Math.max(1, width), (double) out.getHeight() / height) * .9; if (stamp == null) { g.setFont(g.getFont().deriveFont((float) Math.max(1, fontSize * scale))); width = g.getFontMetrics().stringWidth(text); height = g.getFontMetrics().getHeight(); } else { width = Math.max(1, (int) (width * scale)); height = Math.max(1, (int) (height * scale)); } }
        String position = choice(options, "position", "bottom-right", "top-left", "top-right", "center", "bottom-left", "bottom-right", "tile");
        int margin = Math.min(20, Math.min(out.getWidth(), out.getHeight()) / 20);
        int x = position.endsWith("right") ? Math.max(0, out.getWidth() - width - margin) : position.equals("center") ? (out.getWidth() - width) / 2 : margin;
        int y = position.startsWith("bottom") ? Math.max(0, out.getHeight() - height - margin) : position.equals("center") ? (out.getHeight() - height) / 2 : margin;
        g.setColor(color(options, "color", "#ffffff"));
        if (position.equals("tile")) { for (int row = margin; row < out.getHeight(); row += height + 40) for (int col = margin; col < out.getWidth(); col += width + 40) stamp(g, stamp, text, col, row, width, height); }
        else stamp(g, stamp, text, x, y, width, height);
        g.dispose();
        return out;
    }

    private void stamp(Graphics2D g, BufferedImage logo, String text, int x, int y, int width, int height) {
        if (logo != null) g.drawImage(logo, x, y, width, height, null);
        else g.drawString(text, x, y + g.getFontMetrics().getAscent());
    }

    private BufferedImage meme(BufferedImage source, Map<String, String> options) {
        String top = text(options, "topText"), bottom = text(options, "bottomText");
        if (top.isBlank() && bottom.isBlank()) throw new IllegalArgumentException("Enter a top or bottom caption.");
        BufferedImage out = resize(source, source.getWidth(), source.getHeight());
        Graphics2D g = graphics(out);
        int fontSize = integer(options, "fontSize", 40, 8, 300);
        caption(g, top, out.getWidth(), out.getHeight(), fontSize, false);
        caption(g, bottom, out.getWidth(), out.getHeight(), fontSize, true);
        g.dispose();
        return out;
    }

    private void caption(Graphics2D g, String text, int width, int height, int fontSize, boolean bottom) {
        if (text.isBlank()) return;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        while ((g.getFontMetrics().stringWidth(text) > width * .92 || g.getFontMetrics().getHeight() > height / 3) && fontSize > 1) g.setFont(g.getFont().deriveFont((float) --fontSize));
        int x = (width - g.getFontMetrics().stringWidth(text)) / 2;
        int y = bottom ? height - Math.max(2, height / 30) - g.getFontMetrics().getDescent() : Math.max(2, height / 30) + g.getFontMetrics().getAscent();
        Shape outline = g.getFont().createGlyphVector(g.getFontRenderContext(), text).getOutline(x, y);
        g.setStroke(new BasicStroke(Math.max(1, fontSize / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.BLACK); g.draw(outline); g.setColor(Color.WHITE); g.fill(outline);
    }

    private byte[] encode(BufferedImage image, String format, float quality, Color background) throws IOException {
        if (format.equals("jpg")) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics(); g.setColor(background); g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight()); g.drawImage(image, 0, 0, null); g.dispose(); image = rgb;
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (format.equals("jpg")) { param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); param.setCompressionQuality(quality); }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally { writer.dispose(); }
        return bytes.toByteArray();
    }

    private byte[] gif(List<BufferedImage> frames, int delay, boolean loop) throws IOException {
        int width = frames.get(0).getWidth(), height = frames.get(0).getHeight();
        for (BufferedImage frame : frames) if (frame.getWidth() != width || frame.getHeight() != height) throw new IllegalArgumentException("GIF frames must have the same dimensions. Resize them first.");
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output); writer.prepareWriteSequence(null);
            for (int index = 0; index < frames.size(); index++) {
                var metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frames.get(index)), writer.getDefaultWriteParam());
                var root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
                var control = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod", "none"); control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE"); control.setAttribute("delayTime", String.valueOf(Math.max(2, Math.round(delay / 10f)))); control.setAttribute("transparentColorIndex", "0");
                if (index == 0 && loop) { var extensions = new IIOMetadataNode("ApplicationExtensions"); var app = new IIOMetadataNode("ApplicationExtension"); app.setAttribute("applicationID", "NETSCAPE"); app.setAttribute("authenticationCode", "2.0"); app.setUserObject(new byte[] {1, 0, 0}); extensions.appendChild(app); root.appendChild(extensions); }
                metadata.setFromTree("javax_imageio_gif_image_1.0", root);
                writer.writeToSequence(new IIOImage(frames.get(index), null, metadata), null);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
        return bytes.toByteArray();
    }

    private ImageBatchResponse renderHtml(List<MultipartFile> files, Map<String, String> options) {
        if (files.size() != 1 || files.get(0).isEmpty() || files.get(0).getSize() > 500_000) throw new IllegalArgumentException("Upload one HTML file up to 500 KB.");
        int width = integer(options, "width", 1200, 64, 3000), height = integer(options, "height", 800, 64, 3000);
        try {
            String html = new String(files.get(0).getBytes(), StandardCharsets.UTF_8);
            var document = Jsoup.parse(html);
            document.select("script, iframe, object, embed, link, base, meta").remove();
            document.childNodes().stream().filter(node -> node instanceof org.jsoup.nodes.DocumentType).toList().forEach(org.jsoup.nodes.Node::remove);
            document.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
            document.head().appendElement("style").text("@page {size: " + width + "px " + height + "px; margin: 0;} body {margin: 0;}");
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useExternalResourceAccessControl((uri, type) -> false, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI);
            builder.useExternalResourceAccessControl((uri, type) -> false, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
            builder.useUriResolver((baseUri, uri) -> null);
            builder.withHtmlContent(document.html(), null); builder.toStream(pdf); builder.run();
            try (PDDocument pages = PDDocument.load(pdf.toByteArray())) {
                if (pages.getNumberOfPages() > 10 || (long) width * height * pages.getNumberOfPages() > MAX_BATCH_PIXELS) throw new IllegalArgumentException("HTML output exceeds 10 pages or 40 million pixels.");
                PDFRenderer renderer = new PDFRenderer(pages);
                List<ImageFileResultResponse> outputs = new ArrayList<>();
                String format = choice(options, "format", "png", "png", "jpg");
                long totalPixels = 0, totalBytes = 0;
                for (int page = 0; page < pages.getNumberOfPages(); page++) {
                    var box = pages.getPage(page).getCropBox();
                    int pageWidth = (int) Math.ceil(box.getWidth() * 96 / 72), pageHeight = (int) Math.ceil(box.getHeight() * 96 / 72);
                    dimensions(pageWidth, pageHeight);
                    totalPixels += (long) pageWidth * pageHeight;
                    if (totalPixels > MAX_BATCH_PIXELS) throw new IllegalArgumentException("HTML output exceeds 40 million pixels.");
                    BufferedImage image = renderer.renderImageWithDPI(page, 96);
                    dimensions(image.getWidth(), image.getHeight());
                    byte[] bytes = encode(image, format, .85f, Color.WHITE);
                    totalBytes += bytes.length;
                    if (totalBytes > 60L * 1024 * 1024) throw new IllegalArgumentException("HTML output exceeds 60 MB. Reduce the page dimensions or content.");
                    outputs.add(result(stem(files.get(0)) + "-page-" + (page + 1) + "." + format, mime(format), bytes, files.get(0).getSize(), image));
                }
                return new ImageBatchResponse("html-to-image", outputs, List.of("Static HTML/CSS rendering only. Scripts, external images, fonts, and stylesheets are not loaded."));
            }
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalArgumentException("Unable to render this HTML. Use a self-contained HTML file with simple CSS.", error); }
    }

    private ImageFileResultResponse result(String name, String type, byte[] bytes, long originalSize, BufferedImage image) {
        return new ImageFileResultResponse(name, type, bytes.length, originalSize, image.getWidth(), image.getHeight(), Base64.getEncoder().encodeToString(bytes));
    }
    private String stem(MultipartFile file) {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("image").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
        int dot = name.lastIndexOf('.'); if (dot > 0) name = name.substring(0, dot);
        return name.isBlank() ? "image" : name.substring(0, Math.min(100, name.length()));
    }
    private String extension(String format) { return switch (format.toLowerCase(Locale.ROOT)) { case "jpeg" -> "jpg"; case "tif", "tiff" -> "tiff"; default -> format.toLowerCase(Locale.ROOT); }; }
    private String mime(String format) { return "image/" + (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg") ? "jpeg" : extension(format)); }
    private int clamp(double value) { return (int) Math.max(0, Math.min(255, Math.round(value))); }
    private String text(Map<String, String> options, String name) {
        String value = options.getOrDefault(name, "").strip();
        if (value.length() > 300 || value.contains("\n") || value.contains("\r")) throw new IllegalArgumentException(name + " must be a single line of up to 300 characters.");
        return value;
    }
    private String choice(Map<String, String> options, String name, String fallback, String... allowed) {
        String value = options.getOrDefault(name, fallback);
        if (!List.of(allowed).contains(value)) throw new IllegalArgumentException("Invalid " + name + ". Choose " + String.join(", ", allowed) + ".");
        return value;
    }
    private double number(Map<String, String> options, String name, double fallback, double min, double max) {
        try { double value = Double.parseDouble(options.getOrDefault(name, String.valueOf(fallback))); if (Double.isFinite(value) && value >= min && value <= max) return value; }
        catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ".");
    }
    private int integer(Map<String, String> options, String name, int fallback, int min, int max) {
        double value = number(options, name, fallback, min, max);
        if (value != Math.rint(value)) throw new IllegalArgumentException(name + " must be a whole number.");
        return (int) value;
    }
    private boolean bool(Map<String, String> options, String name, boolean fallback) { return choice(options, name, String.valueOf(fallback), "true", "false").equals("true"); }
    private Color color(Map<String, String> options, String name, String fallback) {
        String value = options.getOrDefault(name, fallback);
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException(name + " must be a six-digit HEX color.");
        return Color.decode(value);
    }
}
