package com.compiletime.application;

import com.compiletime.domain.Question;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Loads all question JSON files from the classpath at startup and
 * provides a random question on demand.
 *
 * Questions live in: src/main/resources/questions/*.json
 * Each file is a JSON array of Question objects.
 */
@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final ObjectMapper objectMapper;
    private final List<Question> allQuestions = new ArrayList<>();
    private final Random random = new Random();

    public QuestionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadQuestions() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:questions/*.json");

            for (Resource resource : resources) {
                List<Question> questions = objectMapper.readValue(
                        resource.getInputStream(),
                        new TypeReference<>() {}
                );
                allQuestions.addAll(questions);
                log.info("[QuestionService] Loaded {} questions from {}", questions.size(), resource.getFilename());
            }

            log.info("[QuestionService] Total questions loaded: {}", allQuestions.size());
        } catch (IOException e) {
            log.error("[QuestionService] Failed to load question bank", e);
        }
    }

    /**
     * Returns a random question from the full question bank.
     */
    public Optional<Question> getRandomQuestion() {
        if (allQuestions.isEmpty()) return Optional.empty();
        return Optional.of(allQuestions.get(random.nextInt(allQuestions.size())));
    }

    /**
     * Returns a random question filtered by category (e.g. "docker", "git").
     * Falls back to any random question if the category has no questions.
     */
    public Optional<Question> getRandomQuestion(String category) {
        List<Question> filtered = allQuestions.stream()
                .filter(q -> q.category().equalsIgnoreCase(category))
                .toList();

        if (filtered.isEmpty()) return getRandomQuestion();
        return Optional.of(filtered.get(random.nextInt(filtered.size())));
    }

    public int getTotalCount() {
        return allQuestions.size();
    }
}
