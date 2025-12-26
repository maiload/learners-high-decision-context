package com.example.opa.policydecisionlog.query.app.extractor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RawJsonKeys {

    // Top-level
    public static final String RESULT = "result";

    // Result level
    public static final String ALLOW = "allow";
    public static final String SCORE = "score";
    public static final String POLICIES = "policies";

    // Score level
    public static final String BREAKDOWN = "breakdown";
    public static final String POLICY = "policy";
    public static final String WEIGHT = "weight";

    // Policy level
    public static final String POLICY_NAME = "policy_name";
    public static final String POLICY_DATA = "policy_data";

    // PolicyData / Rule level
    public static final String RESULTS = "results";
    public static final String VIOLATIONS = "violations";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String EXPRESSION = "expression";
}
