package com.sportsmate.server.common.port.out.oauth;

import com.sportsmate.server.common.exception.AuthErrorCode;
import com.sportsmate.server.common.exception.BusinessException;

public class InvalidSocialTokenException extends BusinessException {

    public InvalidSocialTokenException(String detail) {
        super(AuthErrorCode.INVALID_SOCIAL_TOKEN, detail);
    }
}
