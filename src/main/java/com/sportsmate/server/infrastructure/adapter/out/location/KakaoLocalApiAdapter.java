package com.sportsmate.server.infrastructure.adapter.out.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.location.KakaoLocalApiPort;
import com.sportsmate.server.common.port.out.location.LocationRegion;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.infrastructure.monitoring.ExternalDependencyMonitor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoLocalApiAdapter implements KakaoLocalApiPort {

    private static final String URL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";

    private final RestClient restClient;
    private final String restApiKey;
    private final ExternalDependencyMonitor externalDependencyMonitor;

    public KakaoLocalApiAdapter(
            RestClient restClient,
            @Value("${app.kakao.rest-api-key}") String restApiKey,
            ExternalDependencyMonitor externalDependencyMonitor) {
        this.restClient = restClient;
        this.restApiKey = restApiKey;
        this.externalDependencyMonitor = externalDependencyMonitor;
    }

    @Override
    public LocationRegion reverseGeocode(double latitude, double longitude) {
        KakaoRegionResponse response;
        try {
            response = externalDependencyMonitor.observe("kakao-local", () -> restClient.get()
                    .uri(URL + "?x={x}&y={y}", longitude, latitude)
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(KakaoRegionResponse.class));
        } catch (RestClientException e) {
            throw new BusinessException(MemberErrorCode.LOCATION_GEOCODING_FAILED);
        }

        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            throw new BusinessException(MemberErrorCode.LOCATION_GEOCODING_FAILED);
        }

        KakaoRegionResponse.Document document = response.documents().stream()
                .filter(doc -> "H".equals(doc.regionType()))
                .findFirst()
                .orElse(response.documents().getFirst());

        return new LocationRegion(
                document.region1depthName(),
                document.region2depthName(),
                document.region3depthName());
    }

    record KakaoRegionResponse(List<Document> documents) {
        record Document(
                @JsonProperty("region_type") String regionType,
                @JsonProperty("region_1depth_name") String region1depthName,
                @JsonProperty("region_2depth_name") String region2depthName,
                @JsonProperty("region_3depth_name") String region3depthName
        ) {}
    }
}
