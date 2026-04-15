package com.compiletime.api;

import com.compiletime.application.QuestionService;
import com.compiletime.domain.Question;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * Returns a random question. Optionally filter by category.
     * GET /api/questions
     * GET /api/questions?category=docker
     */
    @GetMapping
    public ResponseEntity<Question> getQuestion(
            @RequestParam(required = false) String category) {

        var question = (category != null)
                ? questionService.getRandomQuestion(category)
                : questionService.getRandomQuestion();

        return question
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
