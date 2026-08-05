package com.example.baza.Service;

import com.example.baza.Dto.KatmSorovDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * KATM so'rovini gilam dasturiga (888 Gilam) uzatadi.
 *
 * Gilam tomonda: POST {gilam.url}/api/katm/sorov
 *   sarlavha:  X-Katm-Token: {gilam.token}
 *   javob:     { "message": "...", "holat": true, "sorovId": 12 }
 *
 * Xatolik bo'lsa RuntimeException tashlaydi — chaqiruvchi uni ushlab,
 * sotuvni "yuborilmadi" deb belgilaydi va keyin qayta yuborish mumkin bo'ladi.
 */
@Service
public class GilamKlient {

    private static final Logger log = LoggerFactory.getLogger(GilamKlient.class);

    private final RestClient restClient = RestClient.create();

    @Value("${gilam.url:http://localhost:8081}")
    private String gilamUrl;

    @Value("${gilam.token:}")
    private String token;

    @Value("${gilam.enabled:true}")
    private boolean enabled;

    /** @return gilamdagi so'rov id'si (bo'lmasa null) */
    @SuppressWarnings("unchecked")
    public Long sorovYuborish(KatmSorovDto dto) {
        if (!enabled) {
            throw new IllegalStateException("Gilam integratsiyasi o'chirilgan (gilam.enabled=false)");
        }
        String url = gilamUrl.replaceAll("/+$", "") + "/api/katm/sorov";

        Map<String, Object> javob = restClient.post()
                .uri(url)
                .header("X-Katm-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(Map.class);

        if (javob == null) {
            throw new IllegalStateException("Gilamdan javob kelmadi");
        }
        Object holat = javob.get("holat");
        if (Boolean.FALSE.equals(holat)) {
            throw new IllegalStateException(String.valueOf(javob.getOrDefault("message", "Gilam rad etdi")));
        }
        log.info("KATM so'rovi gilamga yuborildi: sotuv #{}", dto.sotuvId());

        Object sorovId = javob.get("sorovId");
        return sorovId instanceof Number n ? n.longValue() : null;
    }
}
