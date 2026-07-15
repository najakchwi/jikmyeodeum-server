package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WatchStyleScorer implements MatchScorer {

    @Override
    public String key() {
        return "watchStyle";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.watchStyles().isEmpty() || b.watchStyles().isEmpty()) {
            return 0.5;
        }
        Set<WatchStyle> union = new HashSet<>(a.watchStyles());
        union.addAll(b.watchStyles());
        Set<WatchStyle> intersection = new HashSet<>(a.watchStyles());
        intersection.retainAll(b.watchStyles());
        return (double) intersection.size() / union.size();
    }
}
