package com.sportsmate.server.infrastructure.adapter.out.storage.exception;

import com.sportsmate.server.common.exception.BusinessException;

public class ObjectNotFoundException extends BusinessException {

    public ObjectNotFoundException(String objectKey) {
        super(ObjectStorageErrorCode.OBJECT_NOT_FOUND, "objectKey=" + objectKey);
    }
}
