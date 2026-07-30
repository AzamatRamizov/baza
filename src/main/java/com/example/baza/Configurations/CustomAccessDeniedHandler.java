package com.example.baza.Configurations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @PreAuthorize o'tmaganda (ruxsat yo'q) ishlaydi.
 *  - AJAX so'rovi  -> 403 + ApiResponse JSON (sahifa o'zi notification chiqaradi)
 *  - Sahifa so'rovi -> dashboardga redirect + ?xabar=...&tur=err (notification chiqadi)
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final String SAHIFA_XABAR = "Sizda bu sahifaga kirish ruxsati yo\u2018q";
    private static final String AMAL_XABAR = "Sizda bu amal uchun ruxsat yo\u2018q";

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        if (SorovTuri.ajax(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"message\":\"" + AMAL_XABAR + "\",\"holat\":false}");
            response.getWriter().flush();
            return;
        }

        String xabar = URLEncoder.encode(SAHIFA_XABAR, StandardCharsets.UTF_8);
        response.sendRedirect("/admin/dashboard?xabar=" + xabar + "&tur=err");
    }
}
