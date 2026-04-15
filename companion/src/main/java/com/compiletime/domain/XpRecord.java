package com.compiletime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "xp_record")
public class XpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "time_ms")
    private Integer timeMs;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected XpRecord() {}

    public XpRecord(Long sessionId, String questionId, boolean correct, Integer timeMs, int xpAwarded) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.correct = correct;
        this.timeMs = timeMs;
        this.xpAwarded = xpAwarded;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getQuestionId() { return questionId; }
    public boolean isCorrect() { return correct; }
    public Integer getTimeMs() { return timeMs; }
    public int getXpAwarded() { return xpAwarded; }
    public Instant getRecordedAt() { return recordedAt; }
}
