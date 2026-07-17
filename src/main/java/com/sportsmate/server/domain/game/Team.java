package com.sportsmate.server.domain.game;

public record Team(Long id, Long leagueId, String name, String shortName, String emblemImageKey,
        String primaryColorHex) {
}
