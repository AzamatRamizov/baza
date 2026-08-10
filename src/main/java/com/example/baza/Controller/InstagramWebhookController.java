package com.example.baza.Controller;

import com.example.baza.Service.ArizaService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Meta (Instagram/Facebook) Lead Ads webhook qabul qiluvchisi.
 *
 * Ochiq (autentifikatsiyasiz) endpoint — Config.java'da /admin/** dan
 * tashqari hamma manzil permitAll, shuning uchun bu yerga alohida sozlash
 * shart emas. Xavfsizlik X-Hub-Signature-256 imzosi bilan ta'minlanadi.
 *
 * Meta'da sozlash: Meta Developer App > Webhooks > Page > "leadgen"
 * mavzusiga obuna, Callback URL = https://SIZNING_DOMEN/webhook/instagram-lead
 */
@RestController
@RequestMapping("/webhook/instagram-lead")
public class InstagramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(InstagramWebhookController.class);

    private final ArizaService arizaService;
    private final ObjectMapper objectMapper;

    @Value("${instagram.verify-token:}")
    private String verifyToken;

    @Value("${instagram.app-secret:}")
    private String appSecret;

    public InstagramWebhookController(ArizaService arizaService, ObjectMapper objectMapper) {
        this.arizaService = arizaService;
        this.objectMapper = objectMapper;
    }

    /** Meta'ning bir martalik tekshiruv so'rovi (webhook'ni ulaganda) */
    @GetMapping
    public ResponseEntity<String> tekshiruv(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken != null && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verify token mos kelmadi");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> qabulQilish(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @org.springframework.web.bind.annotation.RequestBody String rawBody) {

        if (!imzoTogri(rawBody, signature)) {
            log.warn("Instagram webhook: imzo mos kelmadi, so'rov rad etildi");
            return ResponseEntity.status(401).body("Imzo mos kelmadi");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            for (JsonNode entry : root.path("entry")) {
                String entryPageId = entry.path("id").asText(null);

                for (JsonNode change : entry.path("changes")) {
                    if (!"leadgen".equals(change.path("field").asText())) continue;

                    JsonNode value = change.path("value");
                    String leadgenId = value.path("leadgen_id").asText(null);
                    String formId = value.path("form_id").asText(null);
                    String adId = value.path("ad_id").asText(null);
                    String pageId = value.hasNonNull("page_id")
                            ? value.path("page_id").asText(null) : entryPageId;

                    arizaService.webhookdanSaqlash(pageId, leadgenId, formId, adId);
                }
            }
        } catch (Exception e) {
            log.error("Instagram webhook payload'ni qayta ishlashda xatolik: {}", e.getMessage());
        }

        // Meta har doim 200 kutadi — aks holda qayta-qayta yuborishga urinadi
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    private boolean imzoTogri(String rawBody, String signatureHeader) {
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("instagram.app-secret sozlanmagan — imzo tekshirilmayapti (faqat sozlash bosqichida xavfsiz)");
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String kutilgan = signatureHeader.substring("sha256=".length());

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String hisoblangan = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    hisoblangan.getBytes(StandardCharsets.UTF_8),
                    kutilgan.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Imzoni hisoblashda xatolik: {}", e.getMessage());
            return false;
        }
    }
}
