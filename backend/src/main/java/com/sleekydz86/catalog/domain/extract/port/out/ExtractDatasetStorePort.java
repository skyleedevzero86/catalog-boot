package com.sleekydz86.catalog.domain.extract.port.out;

import com.sleekydz86.catalog.domain.extract.model.ExtractDatasetManifest;
import java.util.Optional;

public interface ExtractDatasetStorePort {

    void save(ExtractDatasetManifest manifest);

    Optional<ExtractDatasetManifest> findById(String datasetId);

    void delete(String datasetId);
}