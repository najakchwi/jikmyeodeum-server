package com.sportsmate.server.infrastructure.adapter.in.web.staticcontent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Static Content", description = "업로드 및 번들 이미지 파일 조회 API")
public class FileController {
    private final Path root = Path.of(System.getProperty("java.io.tmpdir"), "letsports-uploads");

    @GetMapping("/avatars/{memberId}/{fileName:.+}")
    @Operation(summary = "아바타 이미지 파일 조회", description = "로컬 저장소에 업로드된 회원 아바타 이미지 파일을 반환합니다.")
    public ResponseEntity<Resource> avatar(
            @Parameter(description = "회원 ID", example = "1")
            @PathVariable String memberId,
            @Parameter(description = "파일명", example = "profile.png")
            @PathVariable String fileName) throws Exception {
        return file(root.resolve("avatars").resolve(memberId).resolve(fileName).normalize());
    }

    @GetMapping("/images/profile-avatar/{memberId}/{fileName:.+}")
    @Operation(summary = "프로필 아바타 이미지 파일 조회", description = "로컬 저장소에 업로드된 프로필 아바타 이미지 파일을 반환합니다.")
    public ResponseEntity<Resource> profileAvatar(
            @Parameter(description = "회원 ID", example = "1")
            @PathVariable String memberId,
            @Parameter(description = "파일명", example = "profile.png")
            @PathVariable String fileName) throws Exception {
        return file(root.resolve("images/profile-avatar").resolve(memberId).resolve(fileName).normalize());
    }

    @GetMapping("/teams/{fileName:.+}")
    @Operation(summary = "구단 이미지 파일 조회", description = "업로드 저장소 또는 애플리케이션 번들에 있는 구단 이미지 파일을 반환합니다.")
    public ResponseEntity<Resource> team(
            @Parameter(description = "구단 이미지 파일명", example = "lg-twins.svg")
            @PathVariable String fileName) throws Exception {
        Path file = root.resolve("teams").resolve(fileName).normalize();
        if (file.startsWith(root) && Files.exists(file)) {
            return file(file);
        }
        ClassPathResource resource = new ClassPathResource("static/teams/" + fileName);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(mediaType(fileName))
                .body(resource);
    }

    private ResponseEntity<Resource> file(Path file) throws Exception {
        if (!file.startsWith(root) || !Files.exists(file)) return ResponseEntity.notFound().build();
        String contentType = Files.probeContentType(file);
        return ResponseEntity.ok()
                .contentType(contentType == null
                        ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(file));
    }

    private MediaType mediaType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lowerName.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
