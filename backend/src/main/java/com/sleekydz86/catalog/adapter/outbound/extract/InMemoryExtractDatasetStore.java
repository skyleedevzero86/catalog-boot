package com.sleekydz86.catalog.adapter.outbound.extract;

import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import com.sleekydz86.catalog.domain.extract.port.out.ExtractDatasetStorePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "com.sleekydz86.catalog.extract", name = "store", havingValue = "memory")
public class InMemoryExtractDatasetStore implements ExtractDatasetStorePort {

    private final Map<String, ExtractDatasetManifest> manifests = new ConcurrentHashMap<>();

    @Override
    public void save(ExtractDatasetManifest manifest) {
        manifests.put(manifest.datasetId(), manifest);
    }

    @Override
    public Optional<ExtractDatasetManifest> findById(String datasetId) {
        return Optional.ofNullable(manifests.get(datasetId));
    }

    @Override
    public void delete(String datasetId) {
        manifests.remove(datasetId);
    }
}
