package com.example.opa.policydecisionlog.command.infra;

import com.example.opa.policydecisionlog.command.infra.model.DecisionLogRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionLogCommandRepository extends JpaRepository<DecisionLogRow, Long> {
}
