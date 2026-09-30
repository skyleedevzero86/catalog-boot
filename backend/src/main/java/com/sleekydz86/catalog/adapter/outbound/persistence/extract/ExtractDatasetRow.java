package com.sleekydz86.catalog.adapter.outbound.persistence.extract;

public record ExtractDatasetRow(
        String extrDatstId,
        String extrDmndId,
        String extrDatstSttsCd,
        String mnfstCn
) {
}
