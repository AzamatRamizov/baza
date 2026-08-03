package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.SotuvDto;
import com.example.baza.Dto.SotuvSaveDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Entity.OtkazmaHolati;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Sotuv;
import com.example.baza.Entity.SotuvHolati;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.OtkazmaRepository;
import com.example.baza.Repository.SotuvRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Mahsulot sotish.
 *
 * ASOSIY QOIDA: hodim FAQAT o'zi mas'ul bo'lgan magazindagi mahsulotni sotadi.
 * Owner (tizim roli) barcha magazinlar bilan ishlay oladi.
 *
 * Sotilganda:
 *   - mahsulot qoldig'i (miqdor) kamayadi
 *   - sotilgan qismga to'g'ri keladigan zavod narxi (tannarx) ayriladi
 *   - metr / kv.metr uchun bo'yi va kv qayta hisoblanadi (kesib berildi)
 *   - foyda = summa − tannarx
 * Qaytarilganda hammasi teskarisiga tiklanadi.
 */
@Service
public class SotuvService {

    private final SotuvRepository sotuvRepository;
    private final MahsulotRepository mahsulotRepository;
    private final MagazinRepository magazinRepository;
    private final OtkazmaRepository otkazmaRepository;
    private final UsersRepository usersRepository;
    private final ValyutaService valyutaService;
    private final TarixService tarixService;

    /** Sana filtri bo'sh bo'lganda (PostgreSQL null parametr turini aniqlay olmaydi) */
    private static final LocalDateTime SANA_MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SANA_MAX = LocalDateTime.of(2999, 12, 31, 23, 59, 59);
    private static final LocalTime KUN_OXIRI = LocalTime.of(23, 59, 59, 999_000_000);

    public SotuvService(SotuvRepository sotuvRepository,
                        MahsulotRepository mahsulotRepository,
                        MagazinRepository magazinRepository,
                        OtkazmaRepository otkazmaRepository,
                        UsersRepository usersRepository,
                        ValyutaService valyutaService,
                        TarixService tarixService) {
        this.sotuvRepository = sotuvRepository;
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.otkazmaRepository = otkazmaRepository;
        this.usersRepository = usersRepository;
        this.valyutaService = valyutaService;
        this.tarixService = tarixService;
    }

    // ================= RO'YXAT =================

    /**
     * Sotuvlar ro'yxati — hodim uchun faqat o'z magazin(lar)i, Owner uchun barchasi.
     */
    @Transactional(readOnly = true)
    public List<SotuvDto> getSotuvlar(String username, LocalDate sanadan, LocalDate sanagacha) {
        Users u = user(username);
        if (u == null) return List.of();

        LocalDateTime dan = sanadan == null ? SANA_MIN : sanadan.atStartOfDay();
        LocalDateTime gacha = sanagacha == null ? SANA_MAX : sanagacha.atTime(KUN_OXIRI);

        return owner(u)
                ? sotuvRepository.findHammasi(dan, gacha)
                : sotuvRepository.findMeniki(u.getId(), dan, gacha);
    }

    @Transactional(readOnly = true)
    public List<SotuvDto> mahsulotBoyicha(Long mahsulotId) {
        return sotuvRepository.findMahsulotBoyicha(mahsulotId);
    }

    // ================= SOTISH =================

    @Transactional
    public ApiResponse sotish(String username, SotuvSaveDto dto) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);
        if (dto.mahsulotId() == null) {
            return new ApiResponse("Mahsulot tanlanmagan", false);
        }

        Mahsulot mahsulot = mahsulotRepository.findById(dto.mahsulotId()).orElse(null);
        if (mahsulot == null) return new ApiResponse("Mahsulot topilmadi", false);

        Magazin magazin = mahsulot.getMagazin();
        if (magazin == null) {
            return new ApiResponse("Mahsulot hech qaysi magazinga biriktirilmagan", false);
        }
        if (!mansubmi(u, magazin)) {
            return new ApiResponse("Bu mahsulot sizning magazinigizga tegishli emas — " +
                    "faqat o'z magazinizdagi mahsulotni sota olasiz", false);
        }

        // Boshqa magazinga jo'natilib, tasdiq kutayotgan mahsulotni sotib bo'lmaydi
        if (otkazmaRepository.existsByMahsulot_IdAndHolat(mahsulot.getId(), OtkazmaHolati.KUTILMOQDA)) {
            return new ApiResponse("Bu mahsulot boshqa magazinga jo'natilgan va tasdiq kutilmoqda — " +
                    "avval o'tkazmani bekor qiling", false);
        }

        double bor = mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor();
        if (bor <= 0) {
            return new ApiResponse("Mahsulot qoldig'i 0 — sotib bo'lmaydi", false);
        }

        double miqdor = dto.miqdor() == null ? bor : yaxlit(dto.miqdor());
        String xato = miqdorTekshir(miqdor, bor, mahsulot.getBirlik());
        if (xato != null) return new ApiResponse(xato, false);

        Long birlikNarxi = narxniSomga(dto.birlikNarxi(), dto.valyuta());
        if (birlikNarxi == null || birlikNarxi <= 0) {
            return new ApiResponse("USD".equals(dto.valyuta())
                    ? "Dollar kursini olib bo'lmadi — narxni so'mda kiriting"
                    : "Sotuv narxini kiriting", false);
        }

        long summa = Math.round(birlikNarxi * miqdor);
        long tannarx = tannarxUlushi(mahsulot, miqdor, bor);

        // ---- Mahsulot qoldig'ini kamaytiramiz ----
        kamaytir(mahsulot, miqdor, bor, tannarx);
        mahsulotRepository.save(mahsulot);

        // ---- Sotuvni yozamiz ----
        Sotuv s = new Sotuv();
        s.setMahsulot(mahsulot);
        s.setMahsulotNomi(mahsulot.getNomi());
        s.setMahsulotKod(mahsulot.getKod());
        s.setMagazin(magazin);
        s.setMiqdor(yaxlit(miqdor));
        s.setBirlik(mahsulot.getBirlik());
        s.setBirlikNarxi(birlikNarxi);
        s.setSumma(summa);
        s.setTannarx(tannarx);
        s.setFoyda(summa - tannarx);
        s.setMijozIsmi(bosh(dto.mijozIsmi()));
        s.setMijozTel(bosh(dto.mijozTel()));
        s.setIzoh(bosh(dto.izoh()));
        s.setSotgan(u);
        s.setVaqt(LocalDateTime.now());
        s.setHolat(SotuvHolati.SOTILDI);
        sotuvRepository.save(s);

        tarixService.yoz("Sotuv", "Sotildi", s.getId(), mahsulot.getNomi(),
                magazin.getNomi() + " | " + son(miqdor) + " " + birlik(mahsulot.getBirlik()) +
                        " × " + birlikNarxi + " so'm = " + summa + " so'm" +
                        " | Foyda: " + (summa - tannarx) + " so'm" +
                        " | Qoldiq: " + son(mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor()) +
                        (s.getMijozIsmi() == null ? "" : " | Mijoz: " + s.getMijozIsmi()));

        String qoldiqMatn = mahsulot.getMiqdor() != null && mahsulot.getMiqdor() > 0
                ? "qoldiq: " + son(mahsulot.getMiqdor()) + " " + birlik(mahsulot.getBirlik())
                : "mahsulot tugadi";
        return new ApiResponse("Sotildi — " + summa + " so'm (" + qoldiqMatn + ")", true);
    }

    // ================= QAYTARISH =================

    @Transactional
    public ApiResponse qaytarish(String username, Long sotuvId, String sabab) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);

        Sotuv s = sotuvRepository.findById(sotuvId).orElse(null);
        if (s == null) return new ApiResponse("Sotuv topilmadi", false);
        if (s.getHolat() == SotuvHolati.QAYTARILDI) {
            return new ApiResponse("Bu sotuv allaqachon qaytarilgan", false);
        }
        if (!mansubmi(u, s.getMagazin())) {
            return new ApiResponse("Bu sotuv sizning magazinigizga tegishli emas", false);
        }

        Mahsulot mahsulot = s.getMahsulot();
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot o'chirilgan — qaytarib bo'lmaydi", false);
        }

        // Qoldiq va tannarxni tiklaymiz
        kopaytir(mahsulot, s.getMiqdor() == null ? 0 : s.getMiqdor(),
                s.getTannarx() == null ? 0 : s.getTannarx());
        mahsulotRepository.save(mahsulot);

        s.setHolat(SotuvHolati.QAYTARILDI);
        s.setQaytargan(u);
        s.setQaytarilganVaqt(LocalDateTime.now());
        s.setQaytarishSababi(bosh(sabab));
        sotuvRepository.save(s);

        tarixService.yoz("Sotuv", "Qaytarildi", s.getId(), s.getMahsulotNomi(),
                son(s.getMiqdor()) + " " + birlik(s.getBirlik()) + " qaytarildi, " +
                        s.getSumma() + " so'm | Yangi qoldiq: " + son(mahsulot.getMiqdor()) +
                        (sabab == null || sabab.isBlank() ? "" : " | Sabab: " + sabab));

        return new ApiResponse("Sotuv qaytarildi — mahsulot qoldig'i tiklandi", true);
    }

    // ================= QOLDIQ HISOBI =================

    /** Sotilgan miqdorga to'g'ri keladigan zavod narxi */
    private long tannarxUlushi(Mahsulot m, double miqdor, double bor) {
        if (m.getZavodNarxi() == null || bor <= 0) return 0;
        return Math.round(m.getZavodNarxi() * (miqdor / bor));
    }

    /** Mahsulot qoldig'ini kamaytiradi va o'lchamlarni qayta hisoblaydi */
    private void kamaytir(Mahsulot m, double miqdor, double bor, long tannarx) {
        double qoldiq = yaxlit(bor - miqdor);
        m.setMiqdor(qoldiq);

        if (m.getZavodNarxi() != null) {
            m.setZavodNarxi(Math.max(0, m.getZavodNarxi() - tannarx));
        }
        olchamlarniMoslash(m, qoldiq);
    }

    /** Qaytarishda qoldiqni va tannarxni tiklaydi */
    private void kopaytir(Mahsulot m, double miqdor, long tannarx) {
        double bor = m.getMiqdor() == null ? 0 : m.getMiqdor();
        double qoldiq = yaxlit(bor + miqdor);
        m.setMiqdor(qoldiq);
        m.setZavodNarxi((m.getZavodNarxi() == null ? 0 : m.getZavodNarxi()) + tannarx);
        olchamlarniMoslash(m, qoldiq);
    }

    /**
     * Metr / kv.metr mahsulotlarda qoldiq o'zgargach bo'yi va kv ham o'zgaradi
     * (eni saqlanadi). Dona uchun o'lchamlar tegilmaydi.
     */
    private void olchamlarniMoslash(Mahsulot m, double qoldiq) {
        String b = m.getBirlik();
        if (MahsulotService.donami(b)) return;

        if ("metr".equals(b)) {
            m.setBoyi(yaxlit(qoldiq));
        } else if ("kv.metr".equals(b) && m.getEni() != null && m.getEni() > 0) {
            m.setBoyi(yaxlit(qoldiq / m.getEni()));
        }
        if (m.getBoyi() != null && m.getEni() != null) {
            m.setKv(yaxlit(m.getBoyi() * m.getEni()));
        }
    }

    // ================= YORDAMCHI =================

    private String miqdorTekshir(double miqdor, double bor, String birlik) {
        if (miqdor <= 0) {
            return "Miqdor 0 dan katta bo'lishi kerak";
        }
        if (miqdor > bor + 0.0001) {
            return "Mahsulotda faqat " + son(bor) + " " + birlik(birlik) + " bor";
        }
        if (MahsulotService.donami(birlik) && Math.abs(miqdor - Math.round(miqdor)) > 0.0001) {
            return "Dona uchun miqdor butun son bo'lishi kerak";
        }
        return null;
    }

    /** USD bo'lsa CBU kursi bo'yicha so'mga aylantiradi */
    private Long narxniSomga(Double narx, String valyuta) {
        if (narx == null || narx <= 0) return null;
        if (!"USD".equals(valyuta)) return Math.round(narx);

        UsdKursDto kurs = valyutaService.getUsdKurs();
        if (kurs.kurs() == null) return null;
        return Math.round(narx * kurs.kurs());
    }

    /** User shu magazinning mas'ul hodimimi (Owner har doim ha) */
    private boolean mansubmi(Users u, Magazin magazin) {
        if (magazin == null) return false;
        if (owner(u)) return true;
        return magazinRepository.findById(magazin.getId())
                .map(m -> m.getHodimlar().stream()
                        .anyMatch(h -> Objects.equals(h.getId(), u.getId())))
                .orElse(false);
    }

    private boolean owner(Users u) {
        return u.getRollar() != null && u.getRollar().stream().anyMatch(Rol::isTizimRoli);
    }

    private Users user(String username) {
        return username == null ? null : usersRepository.findByUsername(username).orElse(null);
    }

    private String bosh(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private double yaxlit(double son) {
        return Math.round(son * 100.0) / 100.0;
    }

    private Double yaxlit(Double son) {
        return son == null ? null : Math.round(son * 100.0) / 100.0;
    }

    /** 12.0 -> "12",  12.5 -> "12.5" */
    private String son(Double s) {
        if (s == null) return "0";
        return s == Math.rint(s) ? String.valueOf((long) (double) s)
                : String.valueOf(Math.round(s * 100.0) / 100.0);
    }

    private String son(double s) {
        return s == Math.rint(s) ? String.valueOf((long) s)
                : String.valueOf(Math.round(s * 100.0) / 100.0);
    }

    private String birlik(String b) {
        return b == null || b.isBlank() ? "dona" : b;
    }
}
