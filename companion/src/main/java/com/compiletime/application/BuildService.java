package com.compiletime.application;

import com.compiletime.domain.BuildSession;
import com.compiletime.infrastructure.BuildSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BuildService {

    private final BuildSessionRepository repository;

    public BuildService(BuildSessionRepository repository) {
        this.repository = repository;
    }

    public BuildSession startBuild(String command) {
        BuildSession session = new BuildSession(command, Instant.now());
        return repository.save(session);
    }
}
