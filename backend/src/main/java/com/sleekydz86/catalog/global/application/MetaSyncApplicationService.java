package com.sleekydz86.catalog.global.application;


import com.sleekydz86.catalog.domain.metadata.model.MetaSyncResult;
import com.sleekydz86.catalog.domain.metadata.model.SyncMetadataCommand;
import com.sleekydz86.catalog.domain.metadata.service.MetaSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetaSyncApplicationService {

    private final MetaSyncService metaSyncService;

    public MetaSyncApplicationService(MetaSyncService metaSyncService) {
        this.metaSyncService = metaSyncService;
    }

    @Transactional
    public MetaSyncResult sync(SyncMetadataCommand command) {
        return metaSyncService.handle(command);
    }
}
