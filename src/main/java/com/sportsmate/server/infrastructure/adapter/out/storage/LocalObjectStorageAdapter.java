package com.sportsmate.server.infrastructure.adapter.out.storage;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalObjectStorageAdapter implements ObjectStorage {
    private static final String FILE_PATH_PREFIX = "/api/v1/files/";

    private final Path root = Path.of(System.getProperty("java.io.tmpdir"), "letsports-uploads");
    private final String serverUrl;

    public LocalObjectStorageAdapter(@Value("${app.server.url}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public StoredObject upload(ObjectUploadCommand command) {
        try {
            Path target = root.resolve(command.objectKey()).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(command.inputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredObject(command.objectKey(), getUrl(command.objectKey()),
                    command.contentType(), command.contentLength());
        } catch (IOException exception) {
            throw new IllegalStateException("Object upload failed", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(root.resolve(objectKey).normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Object delete failed", exception);
        }
    }

    @Override
    public Optional<byte[]> download(String objectKey) {
        try {
            Path target = root.resolve(objectKey).normalize();
            return Files.exists(target) ? Optional.of(Files.readAllBytes(target)) : Optional.empty();
        } catch (IOException exception) {
            throw new IllegalStateException("Object download failed", exception);
        }
    }

    @Override
    public String getUrl(String objectKey) {
        return serverUrl + FILE_PATH_PREFIX + objectKey;
    }

    @Override
    public String extractKey(String url) {
        int index = url == null ? -1 : url.indexOf(FILE_PATH_PREFIX);
        return index < 0 ? null : url.substring(index + FILE_PATH_PREFIX.length());
    }
}
