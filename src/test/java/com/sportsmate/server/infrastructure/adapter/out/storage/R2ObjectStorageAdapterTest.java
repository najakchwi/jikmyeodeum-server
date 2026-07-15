package com.sportsmate.server.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.infrastructure.monitoring.ExternalDependencyMonitor;
import java.io.ByteArrayInputStream;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@DisplayName("R2ObjectStorageAdapter 단위 테스트")
class R2ObjectStorageAdapterTest {

    private final S3Client s3Client = Mockito.mock(S3Client.class);
    private final ExternalDependencyMonitor externalDependencyMonitor = Mockito.mock(ExternalDependencyMonitor.class);
    private final R2ObjectStorageAdapter adapter = new R2ObjectStorageAdapter(
            s3Client,
            "letsports-dev",
            "https://cdn.example.com/",
            externalDependencyMonitor
    );

    @Test
    @DisplayName("업로드 시 R2 버킷과 object key로 putObject를 호출한다")
    void upload_success() {
        var command = new ObjectUploadCommand(
                "avatars/1/image.png",
                "image/png",
                4,
                new ByteArrayInputStream("test".getBytes())
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        when(externalDependencyMonitor.observe(Mockito.eq("r2"), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

        var result = adapter.upload(command);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("letsports-dev");
        assertThat(requestCaptor.getValue().key()).isEqualTo("avatars/1/image.png");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(result.url()).isEqualTo("https://cdn.example.com/avatars/1/image.png");
    }

    @Test
    @DisplayName("삭제 시 R2 버킷과 object key로 deleteObject를 호출한다")
    void delete_success() {
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        when(externalDependencyMonitor.observe(Mockito.eq("r2"), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

        adapter.delete("avatars/1/image.png");

        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("letsports-dev");
        assertThat(requestCaptor.getValue().key()).isEqualTo("avatars/1/image.png");
    }

    @Test
    @DisplayName("URL 생성 시 publicBaseUrl과 object key를 결합한다")
    void getUrl_success() {
        String url = adapter.getUrl("teams/lg.png");

        assertThat(url).isEqualTo("https://cdn.example.com/teams/lg.png");
    }

    @Test
    @DisplayName("getUrl로 만든 URL에서 extractKey로 objectKey를 다시 추출할 수 있다")
    void extractKey_roundTripsWithGetUrl() {
        String url = adapter.getUrl("avatars/1/profile.png");

        assertThat(adapter.extractKey(url)).isEqualTo("avatars/1/profile.png");
    }

    @Test
    @DisplayName("publicBaseUrl과 일치하지 않는 URL이면 extractKey는 null을 반환한다")
    void extractKey_unknownFormat_returnsNull() {
        assertThat(adapter.extractKey("https://other-cdn.example.com/avatars/1/profile.png")).isNull();
        assertThat(adapter.extractKey(null)).isNull();
    }
}
