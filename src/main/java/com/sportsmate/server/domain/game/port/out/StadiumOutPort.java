package com.sportsmate.server.domain.game.port.out;

import com.sportsmate.server.domain.game.Stadium;
import java.util.Optional;

public interface StadiumOutPort {

    Optional<Stadium> findByKboCode(String kboCode);
}
