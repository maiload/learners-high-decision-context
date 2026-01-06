package com.example.opa.policydecisionlog.query.app.extractor;

import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.PolicyResult;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CloudAccessDecisionExtractorTest {

    private CloudAccessDecisionExtractor extractor;
    private JsonMapper jsonMapper;

    private static final String DENIED_RESPONSE_JSON = """
            {
                "result": {
                    "allow": false,
                    "score": {
                        "breakdown": [
                            {"policy": "vaccine_policy", "weight": 30},
                            {"policy": "required_software", "weight": 50}
                        ]
                    },
                    "policies": [
                        {
                            "policy_name": "vaccine_policy",
                            "policy_data": {
                                "allow": false,
                                "results": [
                                    {
                                        "name": "vaccine",
                                        "type": "vaccine_macos",
                                        "expression": "all",
                                        "allow": false,
                                        "violations": ["File not found: /etc/vaccine"]
                                    },
                                    {
                                        "name": "passed_check",
                                        "type": "check",
                                        "expression": "any",
                                        "allow": true,
                                        "violations": []
                                    }
                                ],
                                "violations": [
                                    {"type": "missing_vaccine", "message": "Vaccine not installed"}
                                ]
                            }
                        },
                        {
                            "policy_name": "required_software",
                            "policy_data": {
                                "allow": false,
                                "results": [
                                    {
                                        "name": "software_check",
                                        "type": "software",
                                        "expression": "any",
                                        "allow": false,
                                        "violations": ["Required software missing"]
                                    }
                                ],
                                "violations": [
                                    {"type": "missing_software", "message": "Software not found"}
                                ]
                            }
                        }
                    ]
                }
            }
            """;

    private static final String ALLOWED_RESPONSE_JSON = """
            {
                "result": {
                    "allow": true,
                    "policies": [
                        {
                            "policy_name": "vaccine_policy",
                            "policy_data": {
                                "allow": true,
                                "violations": []
                            }
                        }
                    ]
                }
            }
            """;

    @BeforeEach
    void setUp() {
        extractor = new CloudAccessDecisionExtractor();
        jsonMapper = JsonMapper.builder().build();
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("cloud_access 서비스면 true 반환")
        void givenCloudAccessService_whenSupports_thenReturnsTrue() {
            assertThat(extractor.supports("cloud_access")).isTrue();
        }

        @Test
        @DisplayName("다른 서비스면 false 반환")
        void givenOtherService_whenSupports_thenReturnsFalse() {
            assertThat(extractor.supports("other_service")).isFalse();
            assertThat(extractor.supports("")).isFalse();
            assertThat(extractor.supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractReasons")
    class ExtractReasons {

        @Test
        @DisplayName("allow가 true면 빈 리스트 반환")
        void givenAllowTrue_whenExtractReasons_thenReturnsEmptyList() {
            // given
            JsonNode raw = jsonMapper.readTree(ALLOWED_RESPONSE_JSON);

            // when
            List<Reason> reasons = extractor.extractReasons(raw);

            // then
            assertThat(reasons).isEmpty();
        }

        @Test
        @DisplayName("result가 없으면 빈 리스트 반환")
        void givenNoResult_whenExtractReasons_thenReturnsEmptyList() {
            // given
            JsonNode raw = jsonMapper.readTree("{}");

            // when
            List<Reason> reasons = extractor.extractReasons(raw);

            // then
            assertThat(reasons).isEmpty();
        }

        @Test
        @DisplayName("violations에서 Reason 추출 및 weight 기준 내림차순 정렬")
        void givenViolations_whenExtractReasons_thenExtractsAndSortsByWeightDesc() {
            // given
            JsonNode raw = jsonMapper.readTree(DENIED_RESPONSE_JSON);

            // when
            List<Reason> reasons = extractor.extractReasons(raw);

            // then
            assertThat(reasons).hasSize(2);

            Reason first = reasons.getFirst();
            assertThat(first.weight()).isEqualTo(50);
            assertThat(first.message()).isEqualTo("Required software missing");
            assertThat(first.rule().policy()).isEqualTo("required_software");

            Reason second = reasons.get(1);
            assertThat(second.weight()).isEqualTo(30);
            assertThat(second.message()).isEqualTo("File not found: /etc/vaccine");
            assertThat(second.rule().policy()).isEqualTo("vaccine_policy");
            assertThat(second.rule().name()).isEqualTo("vaccine");
            assertThat(second.rule().type()).isEqualTo("vaccine_macos");
            assertThat(second.rule().expression()).isEqualTo("all");
        }

        @Test
        @DisplayName("allow가 true인 rule은 건너뜀")
        void givenAllowTrueRule_whenExtractReasons_thenSkipsRule() {
            // given
            JsonNode raw = jsonMapper.readTree(DENIED_RESPONSE_JSON);

            // when
            List<Reason> reasons = extractor.extractReasons(raw);

            // then
            assertThat(reasons).isNotEmpty()
                    .extracting(r -> r.rule().name())
                    .doesNotContain("passed_check");
        }
    }

    @Nested
    @DisplayName("extractPolicies")
    class ExtractPolicies {

        @Test
        @DisplayName("result가 없으면 빈 리스트 반환")
        void givenNoResult_whenExtractPolicies_thenReturnsEmptyList() {
            // given
            JsonNode raw = jsonMapper.readTree("{}");

            // when
            List<PolicyResult> policies = extractor.extractPolicies(raw);

            // then
            assertThat(policies).isEmpty();
        }

        @Test
        @DisplayName("policies에서 PolicyResult 추출")
        void givenPolicies_whenExtractPolicies_thenExtractsPolicyResults() {
            // given
            JsonNode raw = jsonMapper.readTree(DENIED_RESPONSE_JSON);

            // when
            List<PolicyResult> policies = extractor.extractPolicies(raw);

            // then
            assertThat(policies).hasSize(2);

            PolicyResult vaccinePolicy = policies.getFirst();
            assertThat(vaccinePolicy.policy()).isEqualTo("vaccine_policy");
            assertThat(vaccinePolicy.allow()).isFalse();
            assertThat(vaccinePolicy.violationsCount()).isEqualTo(1);

            PolicyResult requiredSoftware = policies.get(1);
            assertThat(requiredSoftware.policy()).isEqualTo("required_software");
            assertThat(requiredSoftware.allow()).isFalse();
            assertThat(requiredSoftware.violationsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("policy_name이 null인 항목은 건너뜀")
        void givenNullPolicyName_whenExtractPolicies_thenSkips() {
            // given
            String json = """
                    {
                        "result": {
                            "policies": [
                                {"policy_data": {"allow": true, "violations": []}},
                                {"policy_name": "valid_policy", "policy_data": {"allow": true, "violations": []}}
                            ]
                        }
                    }
                    """;
            JsonNode raw = jsonMapper.readTree(json);

            // when
            List<PolicyResult> policies = extractor.extractPolicies(raw);

            // then
            assertThat(policies).hasSize(1);
            assertThat(policies.getFirst().policy()).isEqualTo("valid_policy");
        }

        @Test
        @DisplayName("allow가 true여도 policies 추출")
        void givenAllowTrue_whenExtractPolicies_thenStillExtractsPolicies() {
            // given
            JsonNode raw = jsonMapper.readTree(ALLOWED_RESPONSE_JSON);

            // when
            List<PolicyResult> policies = extractor.extractPolicies(raw);

            // then
            assertThat(policies).hasSize(1);
            assertThat(policies.getFirst().policy()).isEqualTo("vaccine_policy");
            assertThat(policies.getFirst().allow()).isTrue();
        }
    }
}
