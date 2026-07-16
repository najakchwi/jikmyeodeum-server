package com.sportsmate.server.infrastructure.adapter.in.web.common.swagger;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * springdoc의 swagger-ui 리소스는 항상 "/api/swagger-ui/index.html"에 고정 마운트되고,
 * 그 index.html은 에셋을 상대경로("./swagger-ui.css" 등)로 참조한다. 이 컨트롤러는 그 index.html에
 * {@code <base>} 태그만 주입해 "/api/docs"에서 직접 서빙함으로써, redirect 없이 주소창을 고정한다.
 */
@RestController
public class SwaggerDocsController {

    private static final String WEBJAR_INDEX_PATTERN =
            "classpath*:/META-INF/resources/webjars/swagger-ui/*/index.html";
    private static final String BASE_TAG = "<base href=\"/api/swagger-ui/\">";

    private final String indexHtml;

    public SwaggerDocsController() {
        this.indexHtml = loadIndexHtmlWithBase();
    }

    @GetMapping(value = "/api/docs", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> swaggerDocs() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(indexHtml);
    }

    /**
     * 실제 마운트 경로("/api/swagger-ui/index.html")로의 직접 접근을 막아 "/api/docs"만 진입점으로 남긴다.
     * 어노테이션 컨트롤러가 정적 리소스 핸들러보다 우선 매칭되므로, 같은 prefix 아래 css/js 에셋은 그대로 서빙된다.
     */
    @GetMapping("/api/swagger-ui/index.html")
    public ResponseEntity<Void> blockDirectIndexAccess() {
        return ResponseEntity.notFound().build();
    }

    private static String loadIndexHtmlWithBase() {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(WEBJAR_INDEX_PATTERN);
            if (resources.length == 0) {
                throw new IllegalStateException("swagger-ui webjar의 index.html을 찾을 수 없습니다.");
            }
            String html = StreamUtils.copyToString(resources[0].getInputStream(), StandardCharsets.UTF_8);
            return html.replaceFirst("(?i)<head>", "<head>\n    " + BASE_TAG);
        } catch (IOException e) {
            throw new UncheckedIOException("swagger-ui index.html 로딩 실패", e);
        }
    }
}
