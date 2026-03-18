package com.mbti.similarity.domain;

import java.util.Objects;

public final class StudentProfile {
    private final String id;
    private final String name;
    private final String sex;
    private final CognitiveScores scores;
    private final String type;
    private final String enneagram;
    private final String nick;

    public StudentProfile(
        String id,
        String name,
        String sex,
        CognitiveScores scores,
        String type,
        String enneagram,
        String nick
    ) {
        this.id = requireText(id, "id");
        this.name = normalizeText(name);
        this.sex = normalizeText(sex);
        this.scores = Objects.requireNonNull(scores, "scores");
        this.type = normalizeText(type);
        this.enneagram = normalizeText(enneagram);
        this.nick = normalizeText(nick);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public CognitiveScores getScores() {
        return scores;
    }

    public String getType() {
        return type;
    }

    public String getEnneagram() {
        return enneagram;
    }

    public String getNick() {
        return nick;
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
