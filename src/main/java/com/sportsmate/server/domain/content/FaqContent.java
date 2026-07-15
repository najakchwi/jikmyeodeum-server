package com.sportsmate.server.domain.content;

public record FaqContent(String code, String category, String question, String answer, int displayOrder) {
}
