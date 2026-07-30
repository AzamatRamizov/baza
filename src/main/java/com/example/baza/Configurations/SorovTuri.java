package com.example.baza.Configurations;

import jakarta.servlet.http.HttpServletRequest;

/**
 * So'rov brauzer sahifasi uchunmi (navigatsiya) yoki fetch/AJAX uchunmi —
 * shuni aniqlaydi. Xato javobini to'g'ri formatda qaytarish uchun kerak:
 *  - sahifa so'rovi  -> redirect (?xabar=... bilan)
 *  - AJAX so'rovi    -> JSON (front-end notification chiqaradi)
 */
public final class SorovTuri {

    private SorovTuri() {
    }

    public static boolean ajax(HttpServletRequest request) {
        // 1) Front-end o'zi belgilab yuboradi (fragment.html'dagi fetch wrapper)
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return true;
        }

        // 2) Brauzer manzil satridan sahifa ochsa — navigate
        if ("navigate".equals(request.getHeader("Sec-Fetch-Mode"))) {
            return false;
        }

        // 3) HTML kutayotgan so'rov — sahifa
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            return false;
        }

        // 4) Qolgani (GET bo'lmagan yoki JSON kutayotgan) — AJAX
        return accept != null && accept.contains("application/json")
                || !"GET".equalsIgnoreCase(request.getMethod());
    }
}
