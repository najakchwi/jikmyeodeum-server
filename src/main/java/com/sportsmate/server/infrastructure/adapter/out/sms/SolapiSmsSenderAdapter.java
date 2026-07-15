package com.sportsmate.server.infrastructure.adapter.out.sms;

import com.sportsmate.server.common.port.out.sms.SmsSender;
import com.sportsmate.server.common.vo.PhoneNumber;
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

    public SolapiSmsSenderAdapter(
            @Value("${app.solapi.api-key}") String apiKey,
            @Value("${app.solapi.secret-key}") String secretKey,
            @Value("${app.solapi.from-number}") String fromNumber) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, secretKey, "https://api.solapi.com");
        this.fromNumber = fromNumber;
    }

    @Override
    public void send(PhoneNumber phoneNumber, String message) {
        Message msg = new Message();
        msg.setFrom(fromNumber);
        msg.setTo(phoneNumber.value());
        msg.setText(message);
        messageService.sendOne(new SingleMessageSendingRequest(msg));
    }
}
