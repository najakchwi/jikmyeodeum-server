package com.sportsmate.server.domain.game.port.out;

import com.sportsmate.server.domain.game.League;
import java.util.List;

public interface LeagueOutPort {
    List<League> findAll();
}
