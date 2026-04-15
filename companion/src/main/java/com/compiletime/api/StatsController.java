package com.compiletime.api;

import com.compiletime.infrastructure.BuildSessionRepository;
import com.compiletime.infrastructure.XpRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final BuildSessionRepository buildSessionRepository;
    private final XpRecordRepository xpRecordRepository;

    public StatsController(BuildSessionRepository buildSessionRepository,
                           XpRecordRepository xpRecordRepository) {
        this.buildSessionRepository = buildSessionRepository;
        this.xpRecordRepository = xpRecordRepository;
    }

    @GetMapping
    public Map<String, Object> getStats() {
        long totalXp = xpRecordRepository.sumXp();
        long questionsAnswered = xpRecordRepository.count();
        long buildsIntercepted = buildSessionRepository.count();

        return Map.of(
                "totalXp", totalXp,
                "questionsAnswered", questionsAnswered,
                "buildsIntercepted", buildsIntercepted
        );
    }
}
