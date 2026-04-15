package com.compiletime.domain;

import java.util.List;

/**
 * Represents a single MCQ question loaded from the JSON question bank.
 * This is a plain Java record — not a JPA entity — because questions are
 * read from JSON files bundled in the JAR, not stored in the database.
 */
public record Question(
        String id,
        String category,
        String difficulty,
        String question,
        List<String> options,
        int correctIndex,
        String explanation,
        int xp,
        List<String> tags
) {}
