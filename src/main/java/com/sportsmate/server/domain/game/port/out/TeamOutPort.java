package com.sportsmate.server.domain.game.port.out;

import com.sportsmate.server.domain.game.Team;
import java.util.List;
import java.util.Optional;

public interface TeamOutPort {

    List<Team> findAll();
    Optional<Team> findByKboCode(String kboCode);
    Optional<Team> findByShortName(String shortName);
}
