package com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.customizer;

import com.sportsmate.server.common.exception.ErrorCode;
import com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.annotation.ApiErrorCodeExample;
import com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.annotation.ApiErrorCodeExamples;
import com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.annotation.ApiSuccessCodeExample;
import com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.annotation.NoErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;

@Component
public class ExampleResponseCustomizer implements OperationCustomizer {

    private static final String APPLICATION_JSON_VALUE = "application/json";
    private static final String EXAMPLE_DATE = "2024-01-01";
    private static final String EXAMPLE_TIME = "00:00:00";
    private static final String EXAMPLE_TIMESTAMP = "2024-01-01T00:00:00";
    private static final int MAX_SAMPLE_DEPTH = 2;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        handleErrorExamples(operation, handlerMethod);
        handleSuccessExample(operation, handlerMethod);
        return operation;
    }

    private void handleErrorExamples(Operation operation, HandlerMethod handlerMethod) {
        List<ApiErrorCodeExample> examples = new ArrayList<>();

        ApiErrorCodeExample single = findAnnotation(handlerMethod, ApiErrorCodeExample.class);
        if (single != null) {
            examples.add(single);
        }

        ApiErrorCodeExamples multiple = findAnnotation(handlerMethod, ApiErrorCodeExamples.class);
        if (multiple != null) {
            examples.addAll(Arrays.asList(multiple.value()));
        }

        Map<String, List<ErrorEntry>> grouped = examples.stream()
                .map(this::resolveErrorEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(entry -> String.valueOf(entry.status()), LinkedHashMap::new, Collectors.toList()));

        grouped.forEach((statusCode, entries) -> {
            Map<String, Example> responseExamples = new LinkedHashMap<>();
            for (ErrorEntry entry : entries) {
                Example example = new Example();
                example.setSummary(entry.message());
                example.setValue(buildErrorBody(entry));
                responseExamples.put(entry.exampleName(), example);
            }

            addExamples(getOrCreateResponse(operation, statusCode, statusDescription(statusCode)), responseExamples);
        });
    }

    private void handleSuccessExample(Operation operation, HandlerMethod handlerMethod) {
        ApiSuccessCodeExample annotation = findAnnotation(handlerMethod, ApiSuccessCodeExample.class);
        if (annotation == null) {
            return;
        }

        String statusCode = findSuccessStatusCode(operation);
        Object exampleData = generateExample(annotation.value());

        Example example = new Example();
        example.setSummary("성공 응답");
        example.setValue(buildSuccessBody(exampleData));

        addExamples(getOrCreateResponse(operation, statusCode, "성공"), Map.of("success", example));
    }

    private ErrorEntry resolveErrorEntry(ApiErrorCodeExample example) {
        if (example.codeType() != NoErrorCode.class) {
            ErrorCode errorCode = Arrays.stream(example.codeType().getEnumConstants())
                    .filter(constant -> constant.name().equals(example.code()))
                    .filter(ErrorCode.class::isInstance)
                    .map(ErrorCode.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid API error code example: " + example.codeType().getSimpleName() + "." + example.code()));

            String exampleName = example.exampleName().isBlank() ? example.code() : example.exampleName();
            return new ErrorEntry(errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode(), exampleName);
        }

        if (example.status() <= 0) {
            return null;
        }

        String code = example.code().isBlank() ? "ERROR_" + example.status() : example.code();
        String exampleName = example.exampleName().isBlank() ? code : example.exampleName();
        String message = example.message().isBlank() ? "에러" : example.message();
        return new ErrorEntry(example.status(), message, code, exampleName);
    }

    private Map<String, Object> buildErrorBody(ErrorEntry entry) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isSuccess", false);
        body.put("data", null);
        body.put("errorCode", entry.code());
        body.put("message", entry.message());
        body.put("timestamp", EXAMPLE_TIMESTAMP);
        return body;
    }

    private Map<String, Object> buildSuccessBody(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isSuccess", true);
        body.put("data", data);
        body.put("errorCode", null);
        body.put("message", null);
        body.put("timestamp", EXAMPLE_TIMESTAMP);
        return body;
    }

    private Object generateExample(Class<?> type) {
        if (type == Void.class || type == void.class) {
            return null;
        }

        return sampleObject(type, 0);
    }

    private Object sampleObject(Class<?> type, int depth) {
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length > 0 ? ((Enum<?>) constants[0]).name() : null;
        }

        if (depth >= MAX_SAMPLE_DEPTH || !type.isRecord()) {
            return Map.of();
        }

        Map<String, Object> sample = new LinkedHashMap<>();
        for (RecordComponent component : type.getRecordComponents()) {
            sample.put(component.getName(), sampleValue(component.getGenericType(), component.getName(), depth + 1));
        }
        return sample;
    }

    private Object sampleValue(Type type, String name, int depth) {
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();

            if (Collection.class.isAssignableFrom(rawType)) {
                Type elementType = parameterizedType.getActualTypeArguments()[0];
                return List.of(sampleValue(elementType, name, depth + 1));
            }

            if (Map.class.isAssignableFrom(rawType)) {
                return Map.of();
            }
        }

        Class<?> clazz = type instanceof Class<?> c ? c : Object.class;

        if (clazz == String.class) {
            return sampleString(name);
        }
        if (clazz == Integer.class || clazz == int.class) {
            return 1;
        }
        if (clazz == Long.class || clazz == long.class) {
            return 1L;
        }
        if (clazz == Double.class || clazz == double.class) {
            return 1.0;
        }
        if (clazz == Float.class || clazz == float.class) {
            return 1.0f;
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return true;
        }
        if (clazz == BigDecimal.class) {
            return new BigDecimal("1000.00");
        }
        if (clazz == BigInteger.class) {
            return new BigInteger("1000");
        }
        if (clazz == LocalDate.class) {
            return LocalDate.parse(EXAMPLE_DATE);
        }
        if (clazz == LocalDateTime.class) {
            return LocalDateTime.parse(EXAMPLE_TIMESTAMP);
        }
        if (clazz == LocalTime.class) {
            return LocalTime.parse(EXAMPLE_TIME);
        }
        if (clazz == Instant.class) {
            return Instant.parse(EXAMPLE_TIMESTAMP + "Z");
        }
        if (clazz == OffsetDateTime.class) {
            return OffsetDateTime.parse(EXAMPLE_TIMESTAMP + "+09:00");
        }
        if (clazz == ZonedDateTime.class) {
            return ZonedDateTime.parse(EXAMPLE_TIMESTAMP + "+09:00[Asia/Seoul]");
        }
        if (clazz == UUID.class) {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return List.of();
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return Map.of();
        }

        return sampleObject(clazz, depth);
    }

    private String sampleString(String name) {
        if (name == null) {
            return "value";
        }

        return switch (name) {
            case "userId", "id", "memberId" -> "member-1";
            case "email" -> "user@example.com";
            case "username", "nickname" -> "letsports";
            case "password" -> "password123";
            case "phoneNumber" -> "01012345678";
            default -> name;
        };
    }

    private void addExamples(ApiResponse response, Map<String, Example> examples) {
        Content content = response.getContent();
        if (content == null) {
            content = new Content();
            response.setContent(content);
        }

        MediaType mediaType = content.get(APPLICATION_JSON_VALUE);
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType(APPLICATION_JSON_VALUE, mediaType);
        }

        Map<String, Example> existingExamples = mediaType.getExamples();
        if (existingExamples == null) {
            existingExamples = new LinkedHashMap<>();
            mediaType.setExamples(existingExamples);
        }

        existingExamples.putAll(examples);
    }

    private ApiResponse getOrCreateResponse(Operation operation, String statusCode, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        ApiResponse response = responses.get(statusCode);
        if (response == null) {
            response = new ApiResponse().description(description);
            responses.addApiResponse(statusCode, response);
        }

        if (response.getDescription() == null || response.getDescription().isBlank()) {
            response.setDescription(description);
        }

        return response;
    }

    private String findSuccessStatusCode(Operation operation) {
        if (operation.getResponses() == null) {
            return "200";
        }

        return operation.getResponses().keySet().stream()
                .filter(code -> code.startsWith("2"))
                .findFirst()
                .orElse("200");
    }

    private String statusDescription(String statusCode) {
        try {
            return HttpStatus.valueOf(Integer.parseInt(statusCode)).getReasonPhrase();
        } catch (Exception e) {
            return "Error";
        }
    }

    private <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        A annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), annotationType);
        if (annotation != null) {
            return annotation;
        }

        for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClass(handlerMethod.getBeanType())) {
            A found = findAnnotationOnInterface(interfaceType, handlerMethod, annotationType);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private <A extends Annotation> A findAnnotationOnInterface(
            Class<?> interfaceType, HandlerMethod handlerMethod, Class<A> annotationType) {
        try {
            Method method = interfaceType.getMethod(
                    handlerMethod.getMethod().getName(), handlerMethod.getMethod().getParameterTypes());
            A annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
            if (annotation != null) {
                return annotation;
            }
        } catch (NoSuchMethodException ignored) {
            // continue searching parent interfaces
        }

        for (Class<?> parentInterface : interfaceType.getInterfaces()) {
            A found = findAnnotationOnInterface(parentInterface, handlerMethod, annotationType);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private record ErrorEntry(int status, String message, String code, String exampleName) {
    }
}
