package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
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
    public static DecisionLogCursorResponse of(List<DecisionLogResponse> content, OffsetDateTime nextCursor) {
        return new DecisionLogCursorResponse(content, nextCursor);
    }

    public static DecisionLogCursorResponse from(List<DecisionLogRow> rows, int limit) {
        boolean hasNext = rows.size() > limit;
        List<DecisionLogRow> content = hasNext ? rows.subList(0, limit) : rows;

        OffsetDateTime nextCursor = hasNext ? content.getLast().ts() : null;

        List<DecisionLogResponse> responses = content.stream()
                .map(DecisionLogResponse::from)
                .toList();

        return DecisionLogCursorResponse.of(responses, nextCursor);
    }
}
