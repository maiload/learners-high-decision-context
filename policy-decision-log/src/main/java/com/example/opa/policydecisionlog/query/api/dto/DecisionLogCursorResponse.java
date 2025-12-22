package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;

import java.time.OffsetDateTime;
import java.util.List;

public record DecisionLogCursorResponse(
        List<DecisionLogResponse> content,
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
