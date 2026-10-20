package com.devtool.service;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.devtool.dto.ImageBatchResponse;

class ImageToolServiceTest {
    private final ImageToolService service = new ImageToolService();
    private MockMultipartFile file(BufferedImage image, String format) throws IOException {
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, format, bytes);
        return new MockMultipartFile("files", "test." + format, "image/" + format, bytes.toByteArray());
    }
    private BufferedImage source() {
        var image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, 40, 20);
        g.setColor(Color.RED); g.fillRect(10, 5, 20, 10); g.dispose(); return image;
    }
    private BufferedImage output(ImageBatchResponse batch) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(batch.files().get(0).base64Content())));
    }
    @Test void resizeCropRotateAndScaleHaveCorrectDimensionsAndPixels() throws Exception {
        var input = file(source(), "png");
        var resized = output(service.process("resize", List.of(input), Map.of("width", "10", "height", "10"), null));
        assertEquals(10, resized.getWidth()); assertEquals(5, resized.getHeight());
        var cropped = output(service.process("crop", List.of(input), Map.of("x", "10", "y", "5", "width", "20", "height", "10"), null));
        assertEquals(20, cropped.getWidth()); assertEquals(Color.RED.getRGB(), cropped.getRGB(5, 5));
        var rotated = output(service.process("rotate", List.of(input), Map.of("angle", "90"), null));
        assertEquals(20, rotated.getWidth()); assertEquals(40, rotated.getHeight());
        assertEquals(80, output(service.process("upscale", List.of(input), Map.of("scale", "2"), null)).getWidth());
        assertThrows(IllegalArgumentException.class, () -> service.process("crop", List.of(input), Map.of("x", "35", "width", "20"), null));
        assertThrows(IllegalArgumentException.class, () -> service.process("rotate", List.of(input), Map.of("angle", "45"), null));
    }
    @Test void backgroundRemovalKeepsEnclosedWhiteDetailAndOutputsTransparency() throws Exception {
        var image = source(); image.setRGB(20, 10, Color.WHITE.getRGB());
        var out = output(service.process("remove-background", List.of(file(image, "png")), Map.of("tolerance", "0"), null));
        assertEquals(0, out.getRGB(0, 0) >>> 24);
        assertEquals(255, out.getRGB(20, 10) >>> 24);
        assertEquals(Color.RED.getRGB(), out.getRGB(11, 6));
    }
    @Test void compressionNeverEnlargesFilesAndConversionFlattensAlpha() throws Exception {
        var input = file(source(), "jpg");
        var compressed = service.process("compress", List.of(input), Map.of("quality", "30"), null);
        assertTrue(compressed.files().get(0).size() <= input.getSize());
        var transparent = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        var out = output(service.process("convert-to-jpg", List.of(file(transparent, "png")), Map.of("background", "#00ff00"), null));
        assertTrue(new Color(out.getRGB(5, 5)).getGreen() > 240);
    }
    @Test void editingWatermarkAndMemeChangePixels() throws Exception {
        var input = file(source(), "png");
        var edited = output(service.process("photo-editor", List.of(input), Map.of("filter", "grayscale"), null));
        var pixel = new Color(edited.getRGB(20, 10)); assertEquals(pixel.getRed(), pixel.getGreen());
        var watermark = output(service.process("watermark", List.of(input), Map.of("text", "TEST", "opacity", "100", "color", "#000000"), null));
        assertTrue(different(source(), watermark));
        var meme = output(service.process("meme", List.of(input), Map.of("topText", "TOP", "bottomText", "BOTTOM"), null));
        assertTrue(different(source(), meme));
        assertThrows(IllegalArgumentException.class, () -> service.process("watermark", List.of(input), Map.of(), null));
    }
    @Test void redactionOnlyAffectsSelectedRegion() throws Exception {
        var image = source(); image.setRGB(1, 1, Color.BLACK.getRGB());
        var out = output(service.process("blur", List.of(file(image, "png")), Map.of("x", "0", "y", "0", "width", "8", "height", "8", "strength", "8"), null));
        assertNotEquals(image.getRGB(1, 1), out.getRGB(1, 1));
        assertEquals(image.getRGB(20, 10), out.getRGB(20, 10));
    }
    @Test void createsAnimatedGifWithDelayAndLoopAndRejectsAnimatedInputs() throws Exception {
        var input = file(source(), "jpg");
        var batch = service.process("convert-from-jpg", List.of(input, input), Map.of("format", "gif", "delay", "200", "loop", "true"), null);
        byte[] bytes = Base64.getDecoder().decode(batch.files().get(0).base64Content());
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var reader = ImageIO.getImageReadersByFormatName("gif").next(); reader.setInput(stream);
            assertEquals(2, reader.getNumImages(true));
            String metadata = "javax_imageio_gif_image_1.0";
            var root = (javax.imageio.metadata.IIOMetadataNode) reader.getImageMetadata(0).getAsTree(metadata);
            assertEquals("20", root.getElementsByTagName("GraphicControlExtension").item(0).getAttributes().getNamedItem("delayTime").getNodeValue());
            assertEquals(1, root.getElementsByTagName("ApplicationExtension").getLength()); reader.dispose();
        }
        var animated = new MockMultipartFile("files", "animation.gif", "image/gif", bytes);
        assertThrows(IllegalArgumentException.class, () -> service.process("resize", List.of(animated), Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> service.process("convert-from-jpg", List.of(file(source(), "png")), Map.of(), null));
    }
    @Test void rendersStaticHtmlToImageWithoutExternalContent() throws Exception {
        var html = new MockMultipartFile("files", "example.html", "text/html", "<html><body style='background:red'><h1>Image tools</h1><script>throw 1</script><img src='file:///etc/passwd'/></body></html>".getBytes());
        var batch = service.process("html-to-image", List.of(html), Map.of("width", "400", "height", "300"), null);
        assertEquals(400, batch.files().get(0).width()); assertEquals(300, batch.files().get(0).height());
        assertFalse(batch.warnings().isEmpty()); assertNotNull(output(batch));
    }
    @Test void rejectsInvalidBytesOptionsAndOversizedDimensionsBeforeDecoding() throws Exception {
        var fake = new MockMultipartFile("files", "fake.png", "image/png", "not an image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.process("resize", List.of(fake), Map.of(), null));
        var input = file(source(), "png");
        assertThrows(IllegalArgumentException.class, () -> service.process("nope", List.of(input), Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> service.process("resize", List.of(input), Map.of("width", "NaN"), null));
        assertThrows(IllegalArgumentException.class, () -> service.process("resize", List.of(input), Map.of("width", "12000", "height", "12000", "keepAspect", "false"), null));
        assertThrows(IllegalArgumentException.class, () -> service.process("remove-background", List.of(input), Map.of("background", "red"), null));
    }
    private boolean different(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x,y) != b.getRGB(x,y)) return true;
        return false;
    }
}
