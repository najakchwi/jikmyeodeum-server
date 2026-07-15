package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.application.matching.filter.DistanceFilter;
import org.springframework.stereotype.Component;

@Component
public class DistanceScorer implements MatchScorer {

    @Override
    public String key() {
        return "distance";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.latitude() == null || a.longitude() == null || b.latitude() == null || b.longitude() == null) {
            return 0.5;
        }
        int maxPreferredDistance = Math.max(1, Math.max(a.distanceKm(), b.distanceKm()));
        double distance = DistanceFilter.distanceKm(a.latitude(), a.longitude(), b.latitude(), b.longitude());
        return Math.max(0.0, 1.0 - (distance / maxPreferredDistance));
    }
}
