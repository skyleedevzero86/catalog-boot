package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.domain.extract.model.ExtractJobStatus;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractWorkerPort;
import com.sleekydz86.catalog.global.config.ExtractWorkerProperties;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HttpExtractWorkerAdapter implements ExtractWorkerPort {

    private static final Logger log = LoggerFactory.getLogger(HttpExtractWorkerAdapter.class);

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
        try {
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
        } catch (RestClientException exception) {
            log.error("Extract Worker 작업 제출 실패 mtdtId={} tableName={}",
                    request.mtdtId(), request.tableName(), exception);
            throw InfrastructureException.of(
                    ErrorCode.EXTERNAL_SERVICE_FAILED,
                    "Extract Worker 작업 제출에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public void cancelExtract(String extractRequestId) {
        if (!properties.enabled()) {
            localStatuses.put(extractRequestId, ExtractJobStatus.CANCELLED);
            return;
        }
        try {
            restTemplate.exchange(
                    properties.baseUrl() + "/api/v1/extract/jobs/" + extractRequestId + "/cancel",
                    HttpMethod.POST,
                    HttpEntity.EMPTY,
                    Void.class
            );
            localStatuses.put(extractRequestId, ExtractJobStatus.CANCELLED);
        } catch (RestClientException exception) {
            log.error("Extract Worker 작업 취소 실패 extractRequestId={}", extractRequestId, exception);
            throw InfrastructureException.of(
                    ErrorCode.EXTERNAL_SERVICE_FAILED,
                    "Extract Worker 작업 취소에 실패했습니다.",
                    exception
            );
        }
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
        } catch (RestClientException exception) {
            log.warn(
                    "Extract Worker 상태 조회 실패 extractRequestId={} cached={} cause={}",
                    extractRequestId,
                    cached,
                    exception.getMessage()
            );
            return cached == null ? ExtractJobStatus.FAILED : cached;
        } catch (IllegalArgumentException exception) {
            log.error("Extract Worker 상태 값 파싱 실패 extractRequestId={}", extractRequestId, exception);
            return ExtractJobStatus.FAILED;
        }
    }
}
