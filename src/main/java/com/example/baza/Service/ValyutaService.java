package com.example.baza.Service;

import com.example.baza.Dto.UsdKursDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * O'zbekiston Markaziy banki (cbu.uz) rasmiy USD kursi.
 * Kurs 60 daqiqa keshda turadi — har so'rovda tashqi API chaqirilmaydi.
 * CBU ishlamay qolsa, oxirgi muvaffaqiyatli kesh qaytadi (eskirgan bo'lsa ham).
 */
@Service
public class ValyutaService {

    private static final String CBU_URL = "https://cbu.uz/uz/arkhiv-kursov-valyut/json/USD/";
    private static final long KESH_DAQIQA = 60;

    private final RestClient restClient = RestClient.create();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private Double keshKurs;
    private String keshSana;
    private LocalDateTime keshVaqti;

    public synchronized UsdKursDto getUsdKurs() {
        boolean keshYangi = keshKurs != null && keshVaqti != null
                && Duration.between(keshVaqti, LocalDateTime.now()).toMinutes() < KESH_DAQIQA;

        if (!keshYangi) {
            yangila();
        }
        return new UsdKursDto(keshKurs, keshSana);
    }

    private void yangila() {
        try {
            String body = restClient.get()
                    .uri(CBU_URL)
                    .retrieve()
                    .body(String.class);

            JsonNode root = jsonMapper.readTree(body);
            if (root.isArray() && !root.isEmpty()) {
                JsonNode usd = root.get(0);
                keshKurs = Double.parseDouble(usd.get("Rate").asText());
                keshSana = usd.get("Date").asText();
                keshVaqti = LocalDateTime.now();
            }
        } catch (Exception e) {
            // CBU'ga ulanib bo'lmadi — eski kesh (bo'lsa) ishlatiladi,
            // keshVaqti yangilanmaydi, keyingi so'rovda qayta uriniladi
            System.err.println("CBU kursini olishda xatolik: " + e.getMessage());
        }
    }
}
