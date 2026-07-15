package com.sportsmate.server.infrastructure.adapter.out.storage;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import com.sportsmate.server.infrastructure.adapter.out.storage.exception.ObjectStorageErrorCode;
import com.sportsmate.server.infrastructure.monitoring.ExternalDependencyMonitor;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Profile("!test")
public class R2ObjectStorageAdapter implements ObjectStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;
    private final ExternalDependencyMonitor externalDependencyMonitor;

    public R2ObjectStorageAdapter(
            S3Client s3Client,
            @Value("${app.r2.bucket}") String bucket,
            @Value("${app.r2.public-base-url}") String publicBaseUrl,
            ExternalDependencyMonitor externalDependencyMonitor
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
        this.externalDependencyMonitor = externalDependencyMonitor;
    }

    @Override
    public StoredObject upload(ObjectUploadCommand command) {
        try {
            // S3Client의 재시도/타임아웃 처리는 요청 바디를 다시 읽기 위해 mark/reset을 요구하는데,
            // MultipartFile.getInputStream()이 반환하는 스트림은 이를 지원하지 않아 바이트로 미리 읽어둔다.
            byte[] bytes = command.inputStream().readAllBytes();
            var request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(command.objectKey())
                    .contentType(command.contentType())
                    .contentLength((long) bytes.length)
                    .build();

            externalDependencyMonitor.observe("r2", () -> s3Client.putObject(request, RequestBody.fromBytes(bytes)));

            return new StoredObject(
                    command.objectKey(),
                    getUrl(command.objectKey()),
                    command.contentType(),
                    bytes.length
            );
        } catch (SdkException exception) {
            throw new BusinessException(ObjectStorageErrorCode.UPLOAD_FAILED, command.objectKey(), exception);
        } catch (IOException exception) {
            throw new BusinessException(ObjectStorageErrorCode.UPLOAD_FAILED, command.objectKey(), exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            externalDependencyMonitor.observe("r2", () -> s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build()));
        } catch (SdkException exception) {
            throw new BusinessException(ObjectStorageErrorCode.DELETE_FAILED, objectKey, exception);
        }
    }

    @Override
    public Optional<byte[]> download(String objectKey) {
        try {
            ResponseBytes<GetObjectResponse> response = externalDependencyMonitor.observe("r2",
                    () -> s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build()));
            return Optional.of(response.asByteArray());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (SdkException exception) {
            throw new BusinessException(ObjectStorageErrorCode.DOWNLOAD_FAILED, objectKey, exception);
        }
    }

    @Override
    public String getUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ObjectStorageErrorCode.URL_GENERATION_FAILED, "objectKey is blank");
        }
        return publicBaseUrl + "/" + objectKey;
    }

    @Override
    public String extractKey(String url) {
        String prefix = publicBaseUrl + "/";
        return url == null || !url.startsWith(prefix) ? null : url.substring(prefix.length());
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ObjectStorageErrorCode.URL_GENERATION_FAILED, "publicBaseUrl is blank");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
