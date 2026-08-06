package com.example.baza.Controller;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KatmJavobKirimDto;
import com.example.baza.Service.SotuvService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gilam dasturi bilan aloqa (login talab qilinmaydi, X-Katm-Token bilan himoyalangan).
 *
 * Oqim:
 *   1. Bu dastur KATM so'rovini gilamga yuboradi (GilamKlient)
 *   2. Gilamda shartnoma tuziladi yoki rad etiladi
 *   3. Gilam shu endpoint'ga javob qaytaradi:
 *        POST /api/katm/javob
 *        X-Katm-Token: <token>
 *        { "sotuvId": 12, "tasdiq": true, "shartnomaId": 345, "izoh": "...", "kim": "Aziz" }
 */
@RestController
@RequestMapping("/api/katm")
public class KatmApiController {

    private final SotuvService sotuvService;

    @Value("${gilam.token:}")
    private String token;

    public KatmApiController(SotuvService sotuvService) {
        this.sotuvService = sotuvService;
    }

    @PostMapping("/javob")
    public ResponseEntity<ApiResponse> javob(
            @RequestHeader(value = "X-Katm-Token", required = false) String kelganToken,
            @RequestBody KatmJavobKirimDto dto) {

        if (token == null || token.isBlank() || !token.equals(kelganToken)) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse("Token noto'g'ri", false));
        }
        if (dto == null) {
            return ResponseEntity.badRequest().body(new ApiResponse("Bo'sh so'rov", false));
        }

        ApiResponse res = sotuvService.tashqiJavob(
                dto.sotuvId(),
                Boolean.TRUE.equals(dto.tasdiq()),
                dto.izoh(),
                dto.shartnomaId(),
                dto.kim());

        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }
}
