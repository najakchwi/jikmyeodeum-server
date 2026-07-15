package com.sportsmate.server.infrastructure.adapter.out.sms;

import com.sportsmate.server.common.port.out.sms.SmsSender;
import com.sportsmate.server.common.vo.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
public class LoggingSmsSenderAdapter implements SmsSender {

    @Override
    public void send(PhoneNumber phoneNumber, String message) {
        log.info("[SMS] to={} message={}", phoneNumber, message);
    }
}
