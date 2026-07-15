package com.sportsmate.server.common.port.out.sms;

import com.sportsmate.server.common.vo.PhoneNumber;

/**
 * 전화번호 로그인을 위한 SMS 발송 포트.
 * 실제 발송 사업자(Coolsms, NHN Cloud 등)가 정해지면 구현 어댑터를 작성한다.
 */
public interface SmsSender {

    void send(PhoneNumber phoneNumber, String message);
}
