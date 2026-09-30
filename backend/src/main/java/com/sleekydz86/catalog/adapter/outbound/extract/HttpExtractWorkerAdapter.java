package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.domain.extract.model.ExtractJobStatus;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractWorkerPort;
import com.sleekydz86.catalog.global.config.ExtractWorkerProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HttpExtractWorkerAdapter implements ExtractWorkerPort {

    private final ExtractWorkerProperties properties;
    private final RestTemplate restTemplate;
    private final Map<String, ExtractJobStatus> localStatuses = new ConcurrentHashMap<>();

    public HttpExtractWorkerAdapter(ExtractWorkerProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public String submitExtract(SubmitExtractRequest request) {
        if (!properties.enabled()) {
            String localId = "local-" + UUID.randomUUID();
            localStatuses.put(localId, ExtractJobStatus.DISABLED);
            return localId;
        }
        String callback = properties.callbackBaseUrl() + "/api/v1/extract/callback";
        Map<String, Object> body = Map.of(
                "mtdtId", request.mtdtId(),
                "tableName", request.tableName(),
                "callbackUrl", callback,
                "actorId", request.actorId() == null ? "system" : request.actorId()
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                properties.baseUrl() + "/api/v1/extract/jobs",
                HttpMethod.POST,
                new HttpEntity<>(body),
                Map.class
        );
        Object jobId = response.getBody() == null ? null : response.getBody().get("jobId");
        String extractRequestId = jobId == null ? UUID.randomUUID().toString() : jobId.toString();
        localStatuses.put(extractRequestId, ExtractJobStatus.SUBMITTED);
        return extractRequestId;
    }

    @Override
    public void cancelExtract(String extractRequestId) {
        if (!properties.enabled()) {
            localStatuses.put(extractRequestId, ExtractJobStatus.CANCELLED);
            return;
        }
        restTemplate.exchange(
                properties.baseUrl() + "/api/v1/extract/jobs/" + extractRequestId + "/cancel",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Void.class
        );
        localStatuses.put(extractRequestId, ExtractJobStatus.CANCELLED);
    }

    @Override
    public ExtractJobStatus getStatus(String extractRequestId) {
        ExtractJobStatus cached = localStatuses.get(extractRequestId);
        if (!properties.enabled()) {
            return cached == null ? ExtractJobStatus.DISABLED : cached;
        }
        if (cached == ExtractJobStatus.CANCELLED) {
            return cached;
        }
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    properties.baseUrl() + "/api/v1/extract/jobs/" + extractRequestId,
                    Map.class
            );
            Object status = response.getBody() == null ? null : response.getBody().get("status");
            if (status == null) {
                return ExtractJobStatus.SUBMITTED;
            }
            return ExtractJobStatus.valueOf(status.toString());
        } catch (RuntimeException exception) {
            return cached == null ? ExtractJobStatus.FAILED : cached;
        }
    }
}
