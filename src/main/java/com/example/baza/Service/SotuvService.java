package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KatmSorovDto;
import com.example.baza.Dto.SotuvDto;
import com.example.baza.Dto.SotuvSaveDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Entity.OtkazmaHolati;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Sotuv;
import com.example.baza.Entity.SotuvHolati;
import com.example.baza.Entity.SotuvTuri;
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
 * IKKI YO'NALISH:
 *   1) NAQD  - oddiy sotuv, holat darhol SOTILDI
 *   2) KATM  - nasiyaga: holat KATMDA bo'ladi, mahsulot band qilinadi (qoldiqdan
 *              chiqadi), lekin tushum/foyda KATM tasdiqlagandan keyin hisoblanadi.
 *              KATM rad etsa - qoldiq to'liq tiklanadi.
 *
 * METRLAB SOTISH (metraj mahsulotlar):
 *   Mahsulot 3 m eni x 100 m bo'yi bo'lsa, sotuvchi 20 metr sotishi mumkin:
 *   kv = 20 x 3 = 60 kv.metr, narx ham 1 KV.METR uchun kiritiladi
 *   => summa = 60 x narx. Eni hech qachon qirqilmaydi, faqat bo'yi.
 *
 * Sotilganda:
 *   - mahsulot qoldig'i (miqdor) kamayadi
 *   - sotilgan qismga to'g'ri keladigan zavod narxi (tannarx) ayriladi
 *   - metr / kv.metr uchun bo'yi va kv qayta hisoblanadi (kesib berildi)
 *   - foyda = summa - tannarx
 * Qaytarilganda (yoki KATM rad etganda) hammasi teskarisiga tiklanadi.
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
    private final GilamKlient gilamKlient;

    /** Sana filtri bo'sh bo'lganda (PostgreSQL null parametr turini aniqlay olmaydi) */
    private static final LocalDateTime SANA_MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SANA_MAX = LocalDateTime.of(2999, 12, 31, 23, 59, 59);
    private static final LocalTime KUN_OXIRI = LocalTime.of(23, 59, 59, 999_000_000);

    private static final double EPS = 0.0001;

    public SotuvService(SotuvRepository sotuvRepository,
                        MahsulotRepository mahsulotRepository,
                        MagazinRepository magazinRepository,
                        OtkazmaRepository otkazmaRepository,
                        UsersRepository usersRepository,
                        ValyutaService valyutaService,
                        TarixService tarixService,
                        GilamKlient gilamKlient) {
        this.sotuvRepository = sotuvRepository;
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.otkazmaRepository = otkazmaRepository;
        this.usersRepository = usersRepository;
        this.valyutaService = valyutaService;
        this.tarixService = tarixService;
        this.gilamKlient = gilamKlient;
    }

    // ================= RO'YXAT =================

    /**
     * Sotuvlar ro'yxati - hodim uchun faqat o'z magazin(lar)i, Owner uchun barchasi.
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

    /**
     * Kassa sahifasi uchun — magazin mansubligidan qat'i nazar HAR DOIM barcha
     * magazinlar bo'yicha (KASSA_KORISH ruxsati aynan shu uchun beriladi —
     * odatiy "Sotuvlar" kabi hodimning o'z magaziniga cheklanmaydi).
     */
    @Transactional(readOnly = true)
    public List<SotuvDto> getKassa(LocalDate sanadan, LocalDate sanagacha) {
        LocalDateTime dan = sanadan == null ? SANA_MIN : sanadan.atStartOfDay();
        LocalDateTime gacha = sanagacha == null ? SANA_MAX : sanagacha.atTime(KUN_OXIRI);
        return sotuvRepository.findHammasi(dan, gacha);
    }

    // ================= SOTISH =================

    /** Oddiy (naqd) sotuv - holat darhol SOTILDI */
    @Transactional
    public ApiResponse sotish(String username, SotuvSaveDto dto) {
        return chiqarish(username, dto, SotuvTuri.NAQD);
    }

    /** KATMga o'tkazish - mahsulot band qilinadi, javob kutiladi */
    @Transactional
    public ApiResponse katmgaOtkazish(String username, SotuvSaveDto dto) {
        return chiqarish(username, dto, SotuvTuri.KATM);
    }

    /**
     * Ikkala yo'nalish uchun umumiy mantiq - farqi faqat turi va boshlang'ich holatda.
     */
    private ApiResponse chiqarish(String username, SotuvSaveDto dto, SotuvTuri turi) {
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
            return new ApiResponse("Bu mahsulot sizning magazinigizga tegishli emas - " +
                    "faqat o'z magazinizdagi mahsulotni sota olasiz", false);
        }

        // Boshqa magazinga jo'natilib, tasdiq kutayotgan mahsulotni sotib bo'lmaydi
        if (otkazmaRepository.existsByMahsulot_IdAndHolat(mahsulot.getId(), OtkazmaHolati.KUTILMOQDA)) {
            return new ApiResponse("Bu mahsulot boshqa magazinga jo'natilgan va tasdiq kutilmoqda - " +
                    "avval o'tkazmani bekor qiling", false);
        }

        double bor = mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor();
        if (bor <= 0) {
            return new ApiResponse("Mahsulot qoldig'i 0 - sotib bo'lmaydi", false);
        }

        // ---- Miqdorni aniqlash: metrajda METR dan, aks holda to'g'ridan-to'g'ri ----
        String birlik = mahsulot.getBirlik();
        double miqdor;
        Double sotilganMetr = null;

        if (dto.metr() != null && dto.metr() > 0 && !MahsulotService.donami(birlik)) {
            if ("kv.metr".equals(birlik) && (mahsulot.getEni() == null || mahsulot.getEni() <= 0)) {
                return new ApiResponse("Mahsulotning eni kiritilmagan - " +
                        "metrlab sotish uchun avval enini to'ldiring", false);
            }
            Double hisob = metrniMiqdorga(mahsulot, dto.metr(), bor);
            if (hisob == null) return new ApiResponse("Metrni miqdorga aylantirib bo'lmadi", false);
            miqdor = hisob;
            sotilganMetr = yaxlit(dto.metr());
        } else {
            miqdor = dto.miqdor() == null ? bor : yaxlit(dto.miqdor());
        }

        String xato = miqdorTekshir(miqdor, bor, birlik);
        if (xato != null) return new ApiResponse(xato, false);

        if (turi == SotuvTuri.KATM && (dto.mijozIsmi() == null || dto.mijozIsmi().isBlank())) {
            return new ApiResponse("KATMga o'tkazish uchun mijoz ismi kiritilishi shart", false);
        }

        long tannarx = tannarxUlushi(mahsulot, miqdor, bor);

        // Sotilgan o'lchamlar
        Double eni = mahsulot.getEni();
        Double boyi = MahsulotService.donami(birlik)
                ? mahsulot.getBoyi()
                : sotilganBoyi(birlik, miqdor, eni, sotilganMetr);

        // Jami kvadrat — NARX SHU BO'YICHA olinadi (dona soniga bog'liq emas):
        //   dona   -> dona soni × (bo'yi × eni)   (3 ta 3x4 gilam = 36 kv)
        //   metraj -> miqdorning o'zi (u allaqachon kv.metrda)
        Double kv = jamiKv(birlik, miqdor, boyi, eni);
        double asos = kv != null && kv > 0 ? kv : miqdor;

        // ---- Narx: yo 1 birlik narxidan, yoki to'g'ridan-to'g'ri UMUMIY summadan ----
        long summa;
        Long birlikNarxi;
        if (dto.umumiySumma() != null && dto.umumiySumma() > 0) {
            Long umumiySomda = narxniSomga(dto.umumiySumma(), dto.valyuta());
            if (umumiySomda == null || umumiySomda <= 0) {
                return new ApiResponse("Dollar kursini olib bo'lmadi - summani so'mda kiriting", false);
            }
            summa = umumiySomda;
            // Faqat yozuv/tarix uchun — orqaga hisoblangan 1 birlik narxi
            birlikNarxi = Math.round(summa / asos);
        } else {
            birlikNarxi = narxniSomga(dto.birlikNarxi(), dto.valyuta());
            if (birlikNarxi == null || birlikNarxi <= 0) {
                return new ApiResponse("USD".equals(dto.valyuta())
                        ? "Dollar kursini olib bo'lmadi - narxni so'mda kiriting"
                        : "Sotuv narxini yoki umumiy summani kiriting", false);
            }
            // Narx 1 kv.metr uchun; o'lchamsiz eski mahsulotlarda esa birlik bo'yicha
            summa = Math.round(birlikNarxi * asos);
        }

        // ---- Mahsulot qoldig'ini kamaytiramiz (KATMda ham band qilinadi) ----
        kamaytir(mahsulot, miqdor, bor, tannarx);
        mahsulotRepository.save(mahsulot);

        // ---- Sotuvni yozamiz ----
        Sotuv s = new Sotuv();
        s.setMahsulot(mahsulot);
        s.setMahsulotNomi(mahsulot.getNomi());
        s.setMahsulotKod(mahsulot.getKod());
        s.setMagazin(magazin);
        s.setMiqdor(yaxlit(miqdor));
        s.setBirlik(birlik);
        s.setBoyi(boyi);
        s.setEni(eni);
        s.setKv(kv);
        s.setBirlikNarxi(birlikNarxi);
        s.setSumma(summa);
        s.setTannarx(tannarx);
        s.setFoyda(summa - tannarx);
        s.setMijozIsmi(bosh(dto.mijozIsmi()));
        s.setMijozTel(bosh(dto.mijozTel()));
        s.setMuddat(dto.muddat() != null && dto.muddat() > 0 ? dto.muddat() : null);
        s.setOldindanTulov(dto.oldindanTulov() != null && dto.oldindanTulov() > 0
                ? dto.oldindanTulov() : null);
        s.setIzoh(bosh(dto.izoh()));
        s.setSotgan(u);
        s.setVaqt(LocalDateTime.now());
        s.setTuri(turi);
        s.setHolat(turi == SotuvTuri.KATM ? SotuvHolati.KATMDA : SotuvHolati.SOTILDI);
        if (turi == SotuvTuri.KATM) {
            s.setKatmVaqti(LocalDateTime.now());
        }
        sotuvRepository.save(s);

        String olcham = olchamMatn(s);
        tarixService.yoz("Sotuv", turi == SotuvTuri.KATM ? "KATMga o'tkazildi" : "Sotildi",
                s.getId(), mahsulot.getNomi(),
                magazin.getNomi() + " | " + son(miqdor) + " " + birlik(birlik) + olcham +
                        " x " + birlikNarxi + " so'm = " + summa + " so'm" +
                        (turi == SotuvTuri.KATM ? " | KATM javobi kutilmoqda"
                                : " | Foyda: " + (summa - tannarx) + " so'm") +
                        " | Qoldiq: " + son(mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor()) +
                        (s.getMijozIsmi() == null ? "" : " | Mijoz: " + s.getMijozIsmi()));

        String qoldiqMatn = mahsulot.getMiqdor() != null && mahsulot.getMiqdor() > 0
                ? "qoldiq: " + son(mahsulot.getMiqdor()) + " " + birlik(birlik)
                : "mahsulot tugadi";

        if (turi != SotuvTuri.KATM) {
            return new ApiResponse("Sotildi - " + summa + " so'm (" + qoldiqMatn + ")", true);
        }

        // KATM: so'rovni gilam dasturiga uzatamiz (tasdiqlash o'sha yerda bo'ladi)
        String xabar = gilamgaYubor(s, magazin);
        return new ApiResponse(xabar + " (" + qoldiqMatn + ")", true);
    }

    // ================= GILAMGA YUBORISH =================

    /**
     * KATM so'rovini gilam dasturiga uzatadi. Xatolik bo'lsa sotuv baribir
     * KATMDA holatida qoladi va "yuborilmadi" deb belgilanadi — keyin
     * sotuvlar sahifasidan "Qayta yuborish" bilan jo'natiladi.
     */
    private String gilamgaYubor(Sotuv s, Magazin magazin) {
        try {
            Long sorovId = gilamKlient.sorovYuborish(sorovDto(s, magazin));
            s.setGilamgaYuborildi(true);
            s.setGilamSorovId(sorovId);
            s.setGilamXato(null);
            sotuvRepository.save(s);
            return "KATM so'rovi gilam dasturiga yuborildi - shartnoma o'sha yerda tuziladi";
        } catch (Exception e) {
            String xato = e.getMessage() == null ? e.toString() : e.getMessage();
            s.setGilamgaYuborildi(false);
            s.setGilamXato(qisqa(xato));
            sotuvRepository.save(s);

            tarixService.yoz("Sotuv", "KATM so'rovi yuborilmadi", s.getId(), s.getMahsulotNomi(), qisqa(xato));
            return "Mahsulot band qilindi, lekin gilamga yuborilmadi: " + qisqa(xato) +
                    " - sotuvlar sahifasidan qayta yuboring";
        }
    }

    /** "Qayta yuborish" tugmasi uchun */
    @Transactional
    public ApiResponse qaytaYuborish(String username, Long sotuvId) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);

        Sotuv s = sotuvRepository.findById(sotuvId).orElse(null);
        if (s == null) return new ApiResponse("Sotuv topilmadi", false);
        if (s.getHolat() != SotuvHolati.KATMDA) {
            return new ApiResponse("Bu sotuv KATM holatida emas", false);
        }
        if (!mansubmi(u, s.getMagazin())) {
            return new ApiResponse("Bu sotuv sizning magazinigizga tegishli emas", false);
        }
        if (Boolean.TRUE.equals(s.getGilamgaYuborildi())) {
            return new ApiResponse("Bu so'rov allaqachon gilamga yuborilgan", false);
        }

        String xabar = gilamgaYubor(s, s.getMagazin());
        return new ApiResponse(xabar, Boolean.TRUE.equals(s.getGilamgaYuborildi()));
    }

    private KatmSorovDto sorovDto(Sotuv s, Magazin magazin) {
        return new KatmSorovDto(
                s.getId(),
                s.getMijozIsmi(), s.getMijozTel(),
                s.getMahsulotNomi(), s.getMahsulotKod(),
                s.getMiqdor(), s.getBirlik(), s.getBoyi(), s.getEni(), s.getKv(),
                s.getTannarx(), s.getSumma(), s.getBirlikNarxi(),
                s.getOldindanTulov(), s.getMuddat(),
                magazin == null ? null : magazin.getNomi(),
                s.getIzoh(),
                s.getSotgan() == null ? null : s.getSotgan().getFish(),
                s.getVaqt());
    }

    private String qisqa(String matn) {
        if (matn == null) return null;
        return matn.length() > 280 ? matn.substring(0, 280) : matn;
    }

    // ================= GILAMDAN KELGAN JAVOB =================

    /**
     * Gilam dasturi javobi (POST /api/katm/javob).
     * Login talab qilinmaydi — token bilan himoyalangan, shuning uchun
     * magazin mansubligi tekshirilmaydi.
     */
    @Transactional
    public ApiResponse tashqiJavob(Long sotuvId, boolean tasdiq, String izoh,
                                   Long shartnomaId, String kim) {
        if (sotuvId == null) return new ApiResponse("sotuvId yo'q", false);

        Sotuv s = sotuvRepository.findById(sotuvId).orElse(null);
        if (s == null) return new ApiResponse("Sotuv topilmadi: " + sotuvId, false);
        if (s.getHolat() != SotuvHolati.KATMDA) {
            return new ApiResponse("Bu sotuv KATM javobini kutmayapti (holat: " + s.getHolat() + ")", false);
        }

        String manba = (kim == null || kim.isBlank() ? "Gilam" : "Gilam / " + kim.trim());
        String toliqIzoh = (shartnomaId == null ? "" : "Shartnoma #" + shartnomaId + ". ") +
                (izoh == null || izoh.isBlank() ? "" : izoh.trim());

        return javobniQollash(s, tasdiq, toliqIzoh.isBlank() ? manba : manba + ": " + toliqIzoh, null);
    }

    // ================= KATM JAVOBI =================

    /**
     * KATM javobi:
     *   tasdiq = true  -> sotuv yakunlanadi (SOTILDI), tushum/foydaga tushadi
     *   tasdiq = false -> rad etiladi (KATM_RAD), mahsulot qoldig'i tiklanadi
     */
    @Transactional
    public ApiResponse katmJavobi(String username, Long sotuvId, boolean tasdiq, String izoh) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);

        Sotuv s = sotuvRepository.findById(sotuvId).orElse(null);
        if (s == null) return new ApiResponse("Sotuv topilmadi", false);
        if (s.getHolat() != SotuvHolati.KATMDA) {
            return new ApiResponse("Bu sotuv KATM javobini kutmayapti", false);
        }
        if (!mansubmi(u, s.getMagazin())) {
            return new ApiResponse("Bu sotuv sizning magazinigizga tegishli emas", false);
        }

        return javobniQollash(s, tasdiq, izoh, u);
    }

    /** KATM javobini qo'llash — ichkaridan ham, gilamdan ham chaqiriladi */
    private ApiResponse javobniQollash(Sotuv s, boolean tasdiq, String izoh, Users u) {
        s.setKatmJavobi(bosh(izoh));
        s.setKatmJavobBergan(u);
        s.setKatmJavobVaqti(LocalDateTime.now());

        if (tasdiq) {
            s.setHolat(SotuvHolati.SOTILDI);
            sotuvRepository.save(s);

            tarixService.yoz("Sotuv", "KATM tasdiqladi", s.getId(), s.getMahsulotNomi(),
                    son(s.getMiqdor()) + " " + birlik(s.getBirlik()) + olchamMatn(s) +
                            " | " + s.getSumma() + " so'm" +
                            (s.getMuddat() == null ? "" : " | Muddat: " + s.getMuddat() + " oy") +
                            (s.getMijozIsmi() == null ? "" : " | Mijoz: " + s.getMijozIsmi()) +
                            (izoh == null || izoh.isBlank() ? "" : " | Izoh: " + izoh));
            return new ApiResponse("KATM tasdiqladi - sotuv yakunlandi", true);
        }

        // Rad etildi - qoldiq va tannarx tiklanadi
        Mahsulot mahsulot = s.getMahsulot();
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot o'chirilgan - qoldiqni tiklab bo'lmaydi", false);
        }
        kopaytir(mahsulot, s.getMiqdor() == null ? 0 : s.getMiqdor(),
                s.getTannarx() == null ? 0 : s.getTannarx());
        mahsulotRepository.save(mahsulot);

        s.setHolat(SotuvHolati.KATM_RAD);
        sotuvRepository.save(s);

        tarixService.yoz("Sotuv", "KATM rad etdi", s.getId(), s.getMahsulotNomi(),
                son(s.getMiqdor()) + " " + birlik(s.getBirlik()) +
                        " qaytdi | Yangi qoldiq: " + son(mahsulot.getMiqdor()) +
                        (izoh == null || izoh.isBlank() ? "" : " | Sabab: " + izoh));

        return new ApiResponse("KATM rad etdi - mahsulot qoldig'i tiklandi", true);
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
        if (s.getHolat() == SotuvHolati.KATM_RAD) {
            return new ApiResponse("KATM rad etgan - mahsulot allaqachon qaytgan", false);
        }
        if (s.getHolat() == SotuvHolati.KATMDA) {
            return new ApiResponse("Bu sotuv KATM javobini kutmoqda - " +
                    "bekor qilish uchun KATM javobini \"rad etildi\" deb belgilang", false);
        }
        if (!mansubmi(u, s.getMagazin())) {
            return new ApiResponse("Bu sotuv sizning magazinigizga tegishli emas", false);
        }

        Mahsulot mahsulot = s.getMahsulot();
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot o'chirilgan - qaytarib bo'lmaydi", false);
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

        return new ApiResponse("Sotuv qaytarildi - mahsulot qoldig'i tiklandi", true);
    }

    // ================= METRAJ HISOBI =================

    /**
     * Metrajda sotuvchi METR kiritadi - uni mahsulot birligidagi miqdorga aylantiramiz:
     *   kv.metr -> metr x eni  (masalan 20 m x 3 m eni = 60 kv.metr)
     *   metr    -> metr
     * Butun qoldiq olinsa - aniq qoldiq qaytariladi (yaxlitlash xatosi bo'lmasin).
     */
    private Double metrniMiqdorga(Mahsulot m, Double metr, double bor) {
        if (metr == null || metr <= 0) return null;

        Double borMetr = m.getBoyi();
        if (borMetr != null && borMetr > 0 && metr >= borMetr - EPS) {
            return yaxlit(bor);   // butun qoldiq
        }
        if ("metr".equals(m.getBirlik())) {
            return yaxlit(metr);
        }
        Double eni = m.getEni();
        if (eni == null || eni <= 0) return null;
        return yaxlit(metr * eni);
    }

    /**
     * Sotilgan bo'yi:
     *   dona    -> mahsulotning bo'yi (o'lcham o'zgarmaydi, faqat soni)
     *   kv.metr -> miqdor / eni (kesib berilgan bo'yi)
     *   metr    -> miqdorning o'zi
     */
    private Double sotilganBoyi(String birlik, double miqdor, Double eni, Double kiritilganMetr) {
        if (MahsulotService.donami(birlik)) return null;   // dona uchun pastda alohida qo'yiladi
        if ("metr".equals(birlik)) return yaxlit(miqdor);
        if (eni != null && eni > 0) return yaxlit(miqdor / eni);
        return kiritilganMetr;
    }

    /**
     * Narx hisoblanadigan JAMI kvadrat:
     *   dona   -> dona soni × (bo'yi × eni)
     *   metraj -> miqdor (kv.metr)
     * O'lchamlar bo'lmasa null (u holda narx birlik bo'yicha olinadi).
     */
    private Double jamiKv(String birlik, double miqdor, Double boyi, Double eni) {
        // kv.metr — miqdorning o'zi allaqachon kvadrat
        if ("kv.metr".equals(birlik)) {
            return yaxlit(miqdor);
        }
        // metr — kvadrat = uzunlik × eni
        if (!MahsulotService.donami(birlik)) {
            if (eni == null || eni <= 0) return null;
            return yaxlit(miqdor * eni);
        }
        // dona — dona soni × bitta parchaning kvadrati
        if (boyi == null || eni == null || boyi <= 0 || eni <= 0) return null;
        return yaxlit(boyi * eni * miqdor);
    }

    /** Tarix/xabar uchun " (3 x 20 m)" ko'rinishidagi qo'shimcha */
    private String olchamMatn(Sotuv s) {
        if (s.getEni() == null || s.getBoyi() == null) return "";
        return " (" + son(s.getEni()) + " x " + son(s.getBoyi()) + " m)";
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

    /** Qaytarishda (yoki KATM rad etganda) qoldiqni va tannarxni tiklaydi */
    private void kopaytir(Mahsulot m, double miqdor, long tannarx) {
        double bor = m.getMiqdor() == null ? 0 : m.getMiqdor();
        double qoldiq = yaxlit(bor + miqdor);
        m.setMiqdor(qoldiq);
        m.setZavodNarxi((m.getZavodNarxi() == null ? 0 : m.getZavodNarxi()) + tannarx);
        olchamlarniMoslash(m, qoldiq);
    }

    /**
     * Metr / kv.metr mahsulotlarda qoldiq o'zgargach bo'yi va kv ham o'zgaradi
     * (eni saqlanadi - eni hech qachon qirqilmaydi). Dona uchun o'lchamlar tegilmaydi.
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
        if (miqdor > bor + EPS) {
            return "Mahsulotda faqat " + son(bor) + " " + birlik(birlik) + " bor";
        }
        if (MahsulotService.donami(birlik) && Math.abs(miqdor - Math.round(miqdor)) > EPS) {
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
