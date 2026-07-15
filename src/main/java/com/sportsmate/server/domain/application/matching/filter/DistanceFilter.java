package com.sportsmate.server.domain.application.matching.filter;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import org.springframework.stereotype.Component;

@Component
public class DistanceFilter implements MatchFilter {

    @Override
    public boolean isEligible(MatchCandidate a, MatchCandidate b) {
        if (!hasLocation(a) || !hasLocation(b)) {
            return true;
        }
        double distance = distanceKm(a.latitude(), a.longitude(), b.latitude(), b.longitude());
        return distance <= a.distanceKm() && distance <= b.distanceKm();
    }

    private boolean hasLocation(MatchCandidate candidate) {
        return candidate.latitude() != null && candidate.longitude() != null;
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
