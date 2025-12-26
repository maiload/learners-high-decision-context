package com.example.opa.policydecisionlog.command.infra;

import com.example.opa.policydecisionlog.command.infra.model.DecisionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionLogJpaRepository extends JpaRepository<DecisionLogEntity, Long> {
}
