package com.devisri.finance.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpTest {

    private final ClientIp clientIp = new ClientIp("CF-Connecting-IP");

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7"); // Render's internal proxy
        return request;
    }

    @Test
    void usesTheIpCloudflareSaw() {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "203.0.113.5");

        assertThat(clientIp.of(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void aFakeXForwardedForHeaderIsIgnored() {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "203.0.113.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.5");

        assertThat(clientIp.of(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void withoutTheHeaderTheDirectConnectionIsUsed() {
        assertThat(clientIp.of(request())).isEqualTo("10.0.0.7");
    }

    @Test
    void aValueThatIsNotAnIpIsIgnored() {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "<script>" + "x".repeat(100));

        assertThat(clientIp.of(request)).isEqualTo("10.0.0.7");
        assertThat(ClientIp.looksLikeIp("2001:db8::1")).isTrue();
    }
}
