package com.sportsmate.server.infrastructure.adapter.out.sms;

import com.sportsmate.server.common.port.out.sms.SmsSender;
import com.sportsmate.server.common.port.out.monitoring.SmsUsagePort;
import com.sportsmate.server.common.vo.PhoneNumber;
import com.sportsmate.server.infrastructure.monitoring.ExternalDependencyMonitor;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SolapiSmsSenderAdapter implements SmsSender {

    private final DefaultMessageService messageService;
    private final String fromNumber;
    private final ExternalDependencyMonitor externalDependencyMonitor;
    private final SmsUsagePort smsUsagePort;

    public SolapiSmsSenderAdapter(
            @Value("${app.solapi.api-key}") String apiKey,
            @Value("${app.solapi.secret-key}") String secretKey,
            @Value("${app.solapi.from-number}") String fromNumber,
            ExternalDependencyMonitor externalDependencyMonitor,
            SmsUsagePort smsUsagePort) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, secretKey, "https://api.solapi.com");
        this.fromNumber = fromNumber;
        this.externalDependencyMonitor = externalDependencyMonitor;
        this.smsUsagePort = smsUsagePort;
    }

    @Override
    public void send(PhoneNumber phoneNumber, String message) {
        try {
            externalDependencyMonitor.observe("solapi-sms", () -> {
                Message msg = new Message();
                msg.setFrom(fromNumber);
                msg.setTo(phoneNumber.value());
                msg.setText(message);
                messageService.sendOne(new SingleMessageSendingRequest(msg));
            });
            recordSent(true);
        } catch (RuntimeException exception) {
            recordSent(false);
            throw exception;
        }
    }

    private void recordSent(boolean success) {
        try {
            smsUsagePort.recordSent(success);
        } catch (RuntimeException exception) {
            // Monitoring must not affect SMS delivery flow.
        }
    }
}
