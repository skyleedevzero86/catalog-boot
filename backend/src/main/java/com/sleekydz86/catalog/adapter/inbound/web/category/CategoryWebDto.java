package com.sleekydz86.catalog.adapter.inbound.web.category;

import com.sleekydz86.catalog.domain.category.model.MetaTableCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class CategoryWebDto {

    private CategoryWebDto() {
    }

    @Schema(name = "CreateCategoryRequest", description = "카테고리 생성 요청")
    public record CreateCategoryRequest(
            @Schema(description = "상위 카테고리 ID (루트면 null)")
            String parentId,
            @Schema(description = "카테고리명", example = "인사")
            @NotBlank String name,
            @Schema(description = "설명")
            String description,
            @Schema(description = "정렬 순서", example = "1")
            Integer sortNo,
            @Schema(description = "노출 여부", example = "true")
            Boolean exposed,
            @Schema(description = "하위 카테고리 허용 여부", example = "true")
            Boolean allowChildCategories
    ) {
    }

    @Schema(name = "UpdateCategoryRequest", description = "카테고리 수정 요청")
    public record UpdateCategoryRequest(
            String parentId,
            @NotBlank String name,
            String description,
            Integer sortNo,
            Boolean exposed,
            Boolean allowChildCategories
    ) {
    }

    @Schema(name = "MapCategoryTablesRequest", description = "카테고리-테이블 매핑 전체 교체 요청")
    public record MapCategoryTablesRequest(
            @Schema(description = "매핑할 메타테이블 ID 목록", example = "[\"mtbl-20260623-001\", \"mtbl-20260623-002\"]")
            @NotNull List<String> tableIds
    ) {
    }

    @Schema(name = "CategoryResponse", description = "카테고리 정보")
    public record CategoryResponse(
            @Schema(example = "ctgr-20260623-001")
            String id,
            @Schema(example = "mtdt-20260623-001")
            String mtdtId,
            String parentId,
            String name,
            String description,
            int sortNo,
            boolean exposed,
            boolean allowChildCategories
    ) {
    }

    static CategoryResponse toResponse(MetaTableCategory category) {
        return new CategoryResponse(
                category.id(),
                category.mtdtId(),
                category.parentId(),
                category.name(),
                category.description(),
                category.sortNo(),
                category.exposed(),
                category.allowChildCategories()
        );
    }
}
