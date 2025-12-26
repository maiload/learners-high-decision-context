package com.example.opa.policydecisionlog.query.app.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CursorPage<T>(
        List<T> content,
        OffsetDateTime nextCursor
) {
}
