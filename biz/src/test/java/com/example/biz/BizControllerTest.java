package com.example.biz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
public class BizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testMangaImageEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/manga-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        System.out.println("Successfully fetched manga image. Size: " + content.length + " bytes.");

        // Basic verification that we got some data
        assertTrue(content.length > 0, "Response content should not be empty");
    }
}
