package com.sleekydz86.catalog.adapter.inbound.web.category;

import com.sleekydz86.catalog.adapter.outbound.persistence.category.MetaTableCategoryMappingRow;
import com.sleekydz86.catalog.global.application.CategoryApplicationService;
import com.sleekydz86.catalog.global.security.AuthenticatedUserProvider;
import com.sleekydz86.catalog.global.config.openapi.OpenApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.sleekydz86.catalog.domain.category.model.*;

@RestController
@RequestMapping("/api/v1/category")
@Tag(name = "04-테이블 카테고리")
public class CategoryController {

    private final CategoryApplicationService categoryApplicationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CategoryController(
            CategoryApplicationService categoryApplicationService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.categoryApplicationService = categoryApplicationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/create/{mtdtId}")
    @Operation(
            summary = "카테고리 생성",
            description = "`sp_mtdt_tbl_ctgr` op=`C` 호출. ID 미지정 시 `ctgr-YYYYMMDD-NNN` 자동 채번."
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CategoryWebDto.CategoryResponse.class)))
    public CategoryWebDto.CategoryResponse create(
            @Parameter(description = "메타데이터세트 ID", example = "mtdt-20260623-001")
            @PathVariable String mtdtId,
            @Valid @RequestBody CategoryWebDto.CreateCategoryRequest request
    ) {
        return CategoryWebDto.toResponse(categoryApplicationService.create(new CreateCategoryCommand(
                mtdtId,
                request.parentId(),
                request.name(),
                request.description(),
                request.sortNo(),
                request.exposed(),
                request.allowChildCategories(),
                authenticatedUserProvider.currentUserId()
        )));
    }

    @PostMapping("/update/{categoryId}")
    @Operation(summary = "카테고리 수정", description = "`sp_mtdt_tbl_ctgr` op=`U` 부분 수정.")
    @OpenApiResponses
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CategoryWebDto.CategoryResponse.class)))
    public CategoryWebDto.CategoryResponse update(
            @Parameter(description = "카테고리 ID", example = "ctgr-20260623-001")
            @PathVariable String categoryId,
            @Valid @RequestBody CategoryWebDto.UpdateCategoryRequest request
    ) {
        return CategoryWebDto.toResponse(categoryApplicationService.update(new UpdateCategoryCommand(
                categoryId,
                request.parentId(),
                request.name(),
                request.description(),
                request.sortNo(),
                request.exposed(),
                request.allowChildCategories(),
                authenticatedUserProvider.currentUserId()
        )));
    }

    @PostMapping("/delete/{categoryId}")
    @Operation(
            summary = "카테고리 삭제",
            description = "`sp_mtdt_tbl_ctgr` op=`D`. 하위 카테고리가 있으면 409 반환."
    )
    @OpenApiResponses
    @ApiResponse(responseCode = "200", description = "삭제 완료")
    public void delete(
            @Parameter(description = "카테고리 ID", example = "ctgr-20260623-001")
            @PathVariable String categoryId
    ) {
        categoryApplicationService.delete(new DeleteCategoryCommand(
                categoryId,
                authenticatedUserProvider.currentUserId()
        ));
    }

    @PostMapping("/map-table/{categoryId}")
    @Operation(
            summary = "카테고리 테이블 매핑 교체",
            description = """
                    카테고리에 연결된 테이블 매핑을 **전체 교체**합니다.
                    1. `sp_mtdt_tbl_ctgr_mpng_clear`로 기존 매핑 삭제
                    2. 요청 `tableIds` 순서대로 `sp_mtdt_tbl_ctgr_mpng` op=`C` 등록
                    """
    )
    @OpenApiResponses
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MetaTableCategoryMappingRow.class)))
    )
    public List<MetaTableCategoryMappingRow> mapTables(
            @Parameter(description = "카테고리 ID", example = "ctgr-20260623-001")
            @PathVariable String categoryId,
            @Valid @RequestBody CategoryWebDto.MapCategoryTablesRequest request
    ) {
        categoryApplicationService.mapTables(new MapCategoryTableCommand(
                categoryId,
                request.tableIds(),
                authenticatedUserProvider.currentUserId()
        ));
        return categoryApplicationService.listMappings(categoryId);
    }

    @GetMapping("/list/{mtdtId}")
    @Operation(summary = "카테고리 목록 조회", description = "메타데이터세트에 속한 카테고리를 `sort_no` 순으로 반환합니다.")
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryWebDto.CategoryResponse.class)))
    )
    public List<CategoryWebDto.CategoryResponse> list(
            @Parameter(description = "메타데이터세트 ID", example = "mtdt-20260623-001")
            @PathVariable String mtdtId
    ) {
        return categoryApplicationService.list(mtdtId).stream()
                .map(CategoryWebDto::toResponse)
                .toList();
    }

}
