package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.SeatZone;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SeatScorer implements MatchScorer {

    @Override
    public String key() {
        return "seat";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.seatZones().isEmpty() || b.seatZones().isEmpty()) {
            return 0.5;
        }
        if (a.seatZones().contains(SeatZone.ANY) || b.seatZones().contains(SeatZone.ANY)) {
            return 0.75;
        }
        Set<SeatZone> union = new HashSet<>(a.seatZones());
        union.addAll(b.seatZones());
        Set<SeatZone> intersection = new HashSet<>(a.seatZones());
        intersection.retainAll(b.seatZones());
        return intersection.isEmpty() ? 0.25 : (double) intersection.size() / union.size();
    }
}
