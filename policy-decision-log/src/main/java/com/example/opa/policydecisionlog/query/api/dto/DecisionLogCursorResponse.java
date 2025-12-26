package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.app.dto.CursorPage;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Decision Log 목록 응답 (Cursor 페이징)")
public record DecisionLogCursorResponse(
        @Schema(description = "Decision Log 목록")
        List<DecisionLogResponse> content,

        @Schema(description = "다음 페이지 커서 (다음 페이지가 없으면 null)")
        OffsetDateTime nextCursor
) {
    public static DecisionLogCursorResponse from(CursorPage<DecisionLogReadModel> page) {
        List<DecisionLogResponse> responses = page.content().stream()
                .map(DecisionLogResponse::from)
                .toList();

        return new DecisionLogCursorResponse(responses, page.nextCursor());
    }
}
