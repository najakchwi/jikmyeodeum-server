package com.sportsmate.server.domain.game.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.application.exception.ApplicationErrorCode;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.Team;
import com.sportsmate.server.domain.game.port.in.GameUseCase;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GameService implements GameUseCase {
    private final GameOutPort gameOutPort;
    private final ApplicationOutPort applicationOutPort;
    private final TeamOutPort teamOutPort;

    public GameService(GameOutPort gameOutPort, ApplicationOutPort applicationOutPort, TeamOutPort teamOutPort) {
        this.gameOutPort = gameOutPort;
        this.applicationOutPort = applicationOutPort;
        this.teamOutPort = teamOutPort;
    }

    @Override
    public CalendarResult calendar(LocalDate startDate, LocalDate endDate) {
        var seasonRange = gameOutPort.findSeasonRange();
        return new CalendarResult(
                gameOutPort.findBetween(startDate, endDate).stream()
                        .map(Game::date).distinct().toList(),
                seasonRange.map(GameOutPort.SeasonRange::startDate).orElse(startDate),
                seasonRange.map(GameOutPort.SeasonRange::endDate).orElse(endDate));
    }

    @Override
    public List<GameResult> games(Long memberId, LocalDate date, List<String> teams) {
        Set<Long> teamIds = teams == null ? Set.of() : teams.stream()
                .map(teamOutPort::findByShortName)
                .flatMap(Optional::stream)
                .map(Team::id)
                .collect(Collectors.toSet());

        return gameOutPort.findByDate(date).stream()
                .filter(game -> teamIds.isEmpty()
                        || teamIds.contains(game.homeTeamId()) || teamIds.contains(game.awayTeamId()))
                .map(game -> toResult(memberId, game))
                .toList();
    }

    @Override
    public GameResult game(Long memberId, String gameId) {
        Game game = gameOutPort.findById(gameId)
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.GAME_NOT_FOUND));
        return toResult(memberId, game);
    }

    public GameResult toResult(Long memberId, Game game) {
        var application = memberId == null ? Optional
                .<com.sportsmate.server.domain.application.Application>empty()
                : applicationOutPort.findByMemberIdAndGameId(memberId, game.id())
                        .filter(found -> !"cancelled".equals(found.getStatus()));
        return new GameResult(
                game.id(), game.homeTeam(), game.awayTeam(), game.stadium(), game.date(),
                game.time().toString(), game.deadline(),
                game.status(application.isPresent(), LocalDate.now()),
                gameOutPort.countWaitingApplications(game.id()), game.homeScore(), game.awayScore(),
                application.map(com.sportsmate.server.domain.application.Application::getId).orElse(null),
                game.homeTeamEmblemUrl(), game.awayTeamEmblemUrl());
    }
}
