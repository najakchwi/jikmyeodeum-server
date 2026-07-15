package com.sportsmate.server.infrastructure.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@Profile("!test")
public class R2Config {

    @Bean
    public S3Client r2S3Client(
            @Value("${app.r2.account-id}") String accountId,
            @Value("${app.r2.access-key-id}") String accessKeyId,
            @Value("${app.r2.secret-access-key}") String secretAccessKey
    ) {
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
    }
}
