package com.sleekydz86.catalog.feature.connection;

import com.sleekydz86.catalog.adapter.inbound.web.connection.ConnectionWebDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("연결 API 기능 테스트")
class ConnectionControllerFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("연결 프로필을 등록하고 목록에서 조회한다")
    void createAndList() throws Exception {
        var request = new ConnectionWebDto.CreateConnectionRequest(
                "Feature Postgres",
                com.sleekydz86.catalog.domain.connection.model.DatabaseVendor.POSTGRESQL,
                "localhost",
                5432,
                "cdw",
                "cdw",
                "feature test",
                "postgres",
                "postgres",
                true
        );

        mockMvc.perform(post("/api/v1/conn/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", "feature-tester")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Feature Postgres"))
                .andExpect(jsonPath("$.healthStatus").value("HEALTHY"));

        mockMvc.perform(get("/api/v1/conn/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Feature Postgres"));
    }
}
