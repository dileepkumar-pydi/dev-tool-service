package com.devtool.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.ImageToolService;

class ImageToolControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ImageToolController(new ImageToolService(), new ApiResponseFactory())).build();
    @Test void multipartEndpointProcessesFilesAndRejectsInvalidDimensions() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var file = new MockMultipartFile("files", "sample.png", "image/png", bytes.toByteArray());
        mvc.perform(multipart("/api/image/resize").file(file).param("width", "10").param("height", "10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.operation").value("resize"))
            .andExpect(jsonPath("$.data.files[0].width").value(10)).andExpect(jsonPath("$.data.files[0].height").value(5))
            .andExpect(jsonPath("$.data.files[0].base64Content").isNotEmpty());
        mvc.perform(multipart("/api/image/crop").file(file).param("x", "80"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status.description").value(org.hamcrest.Matchers.containsString("x must be")));
        mvc.perform(get("/api/image/tools")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(13));
    }
}
