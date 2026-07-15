package com.sportsmate.server.common.port.out.storage;

import java.util.Optional;

/**
 * Object storage(S3, R2 등)를 다루는 인터페이스.
 *
 * HTTP 요청에서 받은 MultipartFile은 web adapter/service 계층에서 ObjectUploadCommand로 변환해서 넘긴다.
 * 어떤 Storage를 쓸 지, 스펙(url, bucket 정보)이 정해지면 ObjectStorage를 구현하는 Adapter를 작성한다.
 */
public interface ObjectStorage {

    StoredObject upload(ObjectUploadCommand command);

    void delete(String objectKey);

    Optional<byte[]> download(String objectKey);

    String getUrl(String objectKey);

    /**
     * getUrl()이 만든 URL에서 objectKey를 역으로 추출한다. 알 수 없는 형식이면 null을 반환한다.
     */
    String extractKey(String url);
}
