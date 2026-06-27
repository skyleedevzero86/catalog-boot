package com.sleekydz86.catalog.feature.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.catalog.adapter.inbound.web.connection.ConnectionWebDto;
import com.sleekydz86.catalog.adapter.inbound.web.migration.MigrationController;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("마이그레이션 API 기능 테스트")
class MigrationControllerFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("배치 적재 후 작업 목록과 상세를 조회한다")
    void batchLoadAndQueryJob() throws Exception {
        String sourceId = createConnection("Migration Source");
        String targetId = createConnection("Migration Target");

        var batchRequest = new MigrationController.BatchLoadRequest(
                sourceId,
                targetId,
                null,
                null,
                "cdw",
                java.util.List.of("EMPLOYEES"),
                100,
                false
        );

        String jobResponse = mockMvc.perform(post("/api/v1/migration/load/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", "feature-tester")
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = objectMapper.readTree(jobResponse).get("jobId").asText();

        mockMvc.perform(get("/api/v1/migration/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId));

        mockMvc.perform(get("/api/v1/migration/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").exists());

        mockMvc.perform(get("/api/v1/migration/jobs/" + jobId + "/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tableName").value("EMPLOYEES"));
    }

    private String createConnection(String name) throws Exception {
        var request = new ConnectionWebDto.CreateConnectionRequest(
                name,
                DatabaseVendor.POSTGRESQL,
                "localhost",
                5432,
                "cdw",
                "cdw",
                "migration test",
                "postgres",
                "postgres",
                true
        );
        String response = mockMvc.perform(post("/api/v1/conn/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", "feature-tester")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("connectionId").asText();
    }
}
