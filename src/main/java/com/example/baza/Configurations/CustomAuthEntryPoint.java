package com.example.baza.Configurations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Login qilinmagan (yoki token muddati tugagan) holat.
 *  - AJAX so'rovi   -> 401 + ApiResponse JSON (front-end login sahifasiga o'tkazadi)
 *  - Sahifa so'rovi -> login sahifasiga redirect + ?xabar=... (login sahifasida chiqadi)
 */
@Component
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private static final String XABAR = "Sessiya muddati tugadi. Qaytadan kiring";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {

        if (SorovTuri.ajax(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"" + XABAR + "\",\"holat\":false}");
            response.getWriter().flush();
            return;
        }

        response.sendRedirect("/?xabar=" + URLEncoder.encode(XABAR, StandardCharsets.UTF_8)
                + "&tur=warn");
    }
}
