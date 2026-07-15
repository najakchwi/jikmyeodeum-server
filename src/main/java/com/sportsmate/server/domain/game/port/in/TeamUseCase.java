package com.sportsmate.server.domain.game.port.in;

import java.util.List;

public interface TeamUseCase {

    List<TeamResult> listTeams();

    record TeamResult(Long id, String name, String shortName, String emblemUrl, String primaryColorHex) {}
}
