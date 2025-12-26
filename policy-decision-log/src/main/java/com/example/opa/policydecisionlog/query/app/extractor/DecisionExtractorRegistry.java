package com.example.opa.policydecisionlog.query.app.extractor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DecisionExtractorRegistry {

    private final List<DecisionExtractor> extractors;
    private final DefaultDecisionExtractor defaultExtractor;

    public DecisionExtractor getExtractor(String service) {
        return extractors.stream()
                .filter(e -> e.supports(service))
                .findFirst()
                .orElse(defaultExtractor);
    }
}
