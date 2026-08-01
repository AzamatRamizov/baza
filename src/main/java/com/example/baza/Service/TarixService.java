package com.example.baza.Service;

import com.example.baza.Dto.TarixDto;
import com.example.baza.Dto.TarixSahifaDto;
import com.example.baza.Entity.Tarix;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.TarixRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Amallar tarixi (audit log).
 *
 * YANGI AMALNI TARIXGA QO'SHISH:
 *   tarixService.yoz("Mahsulot", "Qo'shildi", id, nomi, "qo'shimcha tafsilot");
 * Yozuv joriy foydalanuvchi nomidan yoziladi (SecurityContext'dan).
 */
@Service
public class TarixService {

    public static final int SAHIFA_HAJMI = 30;

    /**
     * Sana filtri bo'sh bo'lganda ishlatiladigan chegaralar.
     * null uzatib bo'lmaydi — TarixRepository.qidir izohiga qarang.
     */
    private static final LocalDateTime SANA_MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SANA_MAX = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    /**
     * Kunning oxiri. LocalTime.MAX (.999999999) ishlatilmaydi — PostgreSQL
     * timestamp'ni mikrosekundgacha yaxlitlab, keyingi kunga o'tkazib yuboradi.
     */
    private static final LocalTime KUN_OXIRI = LocalTime.of(23, 59, 59, 999_000_000);

    private final TarixRepository tarixRepository;
    private final UsersRepository usersRepository;

    public TarixService(TarixRepository tarixRepository, UsersRepository usersRepository) {
        this.tarixRepository = tarixRepository;
        this.usersRepository = usersRepository;
    }

    // ================= YOZISH =================

    /** Joriy foydalanuvchi nomidan tarixga yozadi */
    public void yoz(String bolim, String amal, Long obyektId, String obyektNomi, String tafsilot) {
        yozUser(joriyUsername(), bolim, amal, obyektId, obyektNomi, tafsilot);
    }

    /** Aniq username bilan (masalan login paytida — SecurityContext hali bo'sh) */
    @Transactional
    public void yozUser(String username, String bolim, String amal,
                        Long obyektId, String obyektNomi, String tafsilot) {
        try {
            Tarix t = new Tarix();
            t.setVaqt(LocalDateTime.now());
            t.setBolim(bolim);
            t.setAmal(amal);
            t.setObyektId(obyektId);
            t.setObyektNomi(qirq(obyektNomi, 200));
            t.setTafsilot(qirq(tafsilot, 1500));
            t.setUsername(username);
            if (username != null) {
                usersRepository.findByUsername(username).ifPresent(t::setUser);
            }
            tarixRepository.save(t);
        } catch (Exception e) {
            // Tarix yozilmasa ham asosiy amal buzilmasin
            System.err.println("Tarixga yozib bo'lmadi: " + e.getMessage());
        }
    }

    // ================= O'QISH =================

    @Transactional(readOnly = true)
    public TarixSahifaDto qidir(String bolim, Long userId, String q,
                                LocalDate sanadan, LocalDate sanagacha, int sahifa) {
        Page<TarixDto> page = tarixRepository.qidir(
                bolim == null ? "" : bolim.trim(),
                userId == null ? -1L : userId,
                q == null ? "" : q.trim().toLowerCase(),
                sanadan == null ? SANA_MIN : sanadan.atStartOfDay(),
                sanagacha == null ? SANA_MAX : sanagacha.atTime(KUN_OXIRI),
                PageRequest.of(Math.max(sahifa, 0), SAHIFA_HAJMI));

        return new TarixSahifaDto(page.getContent(), page.getNumber(),
                page.getTotalPages(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<String> getBolimlar() {
        return tarixRepository.findBolimlar();
    }

    // ================= YORDAMCHI =================

    private String joriyUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    /** Joriy foydalanuvchi (boshqa servislar uchun qulay) */
    public Users joriyUser() {
        String username = joriyUsername();
        return username == null ? null : usersRepository.findByUsername(username).orElse(null);
    }

    private String qirq(String s, int uzunlik) {
        if (s == null) return null;
        return s.length() <= uzunlik ? s : s.substring(0, uzunlik - 3) + "...";
    }
}