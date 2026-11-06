package com.devisri.finance.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Finds the visitor's real IP address.
 *
 * On Render, every request first passes through Cloudflare, which puts the IP address that actually
 * connected to it into the "CF-Connecting-IP" header. We use that header, not X-Forwarded-For, because
 * Render's proxy only ADDS to X-Forwarded-For: a visitor can send "X-Forwarded-For: 1.2.3.4" themselves
 * and that fake value stays in the header. Without the header (on your own computer) the address of
 * the direct connection is used.
 */
@Component
public class ClientIp {

    private final String header;

    public ClientIp(@Value("${app.client-ip-header:CF-Connecting-IP}") String header) {
        this.header = header == null ? "" : header.trim();
    }

    public String of(HttpServletRequest request) {
        String value = header.isEmpty() ? null : request.getHeader(header);
        if (value != null) {
            String first = value.split(",")[0].trim();
            if (looksLikeIp(first)) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    /** IPv4 or IPv6 characters only, so a strange header can't become a huge or odd map key. */
    static boolean looksLikeIp(String value) {
        return !value.isEmpty() && value.length() <= 45 && value.matches("[0-9A-Fa-f:.]+");
    }
}
