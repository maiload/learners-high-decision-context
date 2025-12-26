package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.app.dto.CursorPage;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContextSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Decision Context Summary 목록 응답 (Cursor 페이징)")
public record DecisionContextCursorResponse(
        @Schema(description = "Decision Context Summary 목록")
        List<DecisionContextSummary> content,

        @Schema(description = "다음 페이지 커서 (다음 페이지가 없으면 null)")
        OffsetDateTime nextCursor
) {
    public static DecisionContextCursorResponse from(CursorPage<DecisionContextSummary> page) {
        return new DecisionContextCursorResponse(page.content(), page.nextCursor());
    }
}
