package com.devisri.finance.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("unit-test-secret", 7);

    @Test
    void tokenContainsTheUserId() {
        String token = jwt.createToken("user-123");

        assertThat(jwt.readUserId(token)).contains("user-123");
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("a-different-secret", 7).createToken("user-123");

        assertThat(jwt.readUserId(token)).isEmpty();
    }

    @Test
    void brokenTokenIsRejected() {
        assertThat(jwt.readUserId("not-a-real-token")).isEmpty();
    }

    @Test
    void missingSecretStopsTheApp() {
        assertThatThrownBy(() -> new JwtService(" ", 7)).isInstanceOf(IllegalStateException.class);
    }
}
