package com.example.opa.policydecisionlog.command.infra;

import com.example.opa.policydecisionlog.command.app.DecisionLogCommandRepository;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.infra.mapper.CommandToEntityMapper;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DecisionLogCommandRepositoryImpl implements DecisionLogCommandRepository {

    private final DecisionLogJpaRepository jpaRepository;
    private final CommandToEntityMapper mapper;

    @Override
    public void saveAll(List<DecisionLogIngestCommand> commands) {
        List<DecisionLogEntity> entities = commands.stream()
                .map(mapper::toEntity)
                .toList();

        jpaRepository.saveAll(entities);
    }
}
