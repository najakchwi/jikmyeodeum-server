package com.sportsmate.server.common.port.out.token;

import com.sportsmate.server.common.enums.Role;

public interface TokenIssuer {

    TokenPair issue(String memberId, Role role);
}
