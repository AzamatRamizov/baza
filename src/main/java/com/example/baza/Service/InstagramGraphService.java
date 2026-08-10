package com.example.baza.Service;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Meta Graph API'dan Instagram/Facebook Lead Ads arizasining to'liq
 * ma'lumotini oladi. Webhook faqat leadgen_id yuboradi — ism/telefon kabi
 * maydonlarni olish uchun alohida so'rov kerak:
 *
 *   GET {graph-api-base-url}/{leadgenId}?access_token={page-access-token}
 *
 * Javob:
 *   { "id": "...", "form_id": "...", "ad_id": "...",
 *     "field_data": [ {"name":"full_name","values":["..."]},
 *                      {"name":"phone_number","values":["..."]} ] }
 */
@Service
public class InstagramGraphService {

    private static final Logger log = LoggerFactory.getLogger(InstagramGraphService.class);

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    @Value("${instagram.graph-api-base-url:https://graph.facebook.com/v21.0}")
    private String baseUrl;

    public InstagramGraphService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record LeadMalumot(String ismFamiliya, String telefon, String formId,
                              String adId, String xomMalumot) {
    }

    /**
     * @param pageAccessToken shu leadgen kelgan Instagram/Page akkauntining
     *                        Access Token'i (InstagramAkkaunt'dan) — har bir
     *                        akkauntning o'z tokeni bor, umumiy emas
     * @return topilgan ma'lumot, yoki xatolik/token yo'q bo'lsa null
     */
    @SuppressWarnings("unchecked")
    public LeadMalumot leadniOlish(String leadgenId, String pageAccessToken) {
        if (pageAccessToken == null || pageAccessToken.isBlank()) {
            log.warn("Bu akkauntning page access token'i sozlanmagan — lead #{} to'liq olinmadi", leadgenId);
            return null;
        }
        String url = baseUrl.replaceAll("/+$", "") + "/" + leadgenId
                + "?access_token=" + pageAccessToken;

        Map<String, Object> javob;
        try {
            javob = restClient.get().uri(url).retrieve().body(Map.class);
        } catch (Exception e) {
            log.error("Graph API'dan lead #{} olinmadi: {}", leadgenId, e.getMessage());
            return null;
        }
        if (javob == null) return null;

        String ismFamiliya = null;
        String telefon = null;
        Object fieldDataObj = javob.get("field_data");
        if (fieldDataObj instanceof List<?> fieldData) {
            for (Object item : fieldData) {
                if (!(item instanceof Map<?, ?> maydon)) continue;
                String nomi = String.valueOf(maydon.get("name"));
                Object valuesObj = maydon.get("values");
                String qiymat = null;
                if (valuesObj instanceof List<?> values && !values.isEmpty()) {
                    qiymat = String.valueOf(values.get(0));
                }
                if (qiymat == null) continue;

                if (nomi.contains("phone")) {
                    telefon = qiymat;
                } else if (nomi.contains("name")) {
                    ismFamiliya = qiymat;
                }
            }
        }

        String xomMalumot;
        try {
            xomMalumot = objectMapper.writeValueAsString(javob);
        } catch (Exception e) {
            xomMalumot = String.valueOf(javob);
        }

        return new LeadMalumot(
                ismFamiliya,
                telefon,
                javob.get("form_id") == null ? null : String.valueOf(javob.get("form_id")),
                javob.get("ad_id") == null ? null : String.valueOf(javob.get("ad_id")),
                xomMalumot);
    }
}
