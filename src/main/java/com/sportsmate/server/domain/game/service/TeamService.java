package com.sportsmate.server.domain.game.service;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.domain.game.port.in.TeamUseCase;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeamService implements TeamUseCase {

    private final TeamOutPort teamOutPort;
    private final ObjectStorage objectStorage;

    public TeamService(TeamOutPort teamOutPort, ObjectStorage objectStorage) {
        this.teamOutPort = teamOutPort;
        this.objectStorage = objectStorage;
    }

    @Override
    public List<TeamResult> listTeams() {
        return teamOutPort.findAll().stream()
                .map(team -> new TeamResult(
                        team.id(),
                        team.name(),
                        team.shortName(),
                        team.emblemImageKey() == null ? null : objectStorage.getUrl(team.emblemImageKey()),
                        team.primaryColorHex()))
                .toList();
    }
}
