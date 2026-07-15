package com.sportsmate.server.common.port.out.location;

public interface KakaoLocalApiPort {
    LocationRegion reverseGeocode(double latitude, double longitude);
}
