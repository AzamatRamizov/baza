package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.OtkazmaDto;
import com.example.baza.Dto.OtkazmaSaveDto;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Entity.Otkazma;
import com.example.baza.Entity.OtkazmaHolati;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.OtkazmaRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Mahsulotni bir magazindan boshqasiga o'tkazish.
 *
 * Qoidalar:
 *  - Jo'natish: mahsulot turgan magazinning mas'ul hodimi (yoki Owner)
 *  - Qabul / rad etish: qabul qiluvchi magazinning mas'ul hodimi (yoki Owner)
 *  - Bitta mahsulot bo'yicha bir vaqtda faqat bitta kutilayotgan o'tkazma
 *  - Mahsulotning magazini FAQAT tasdiqlangandan keyin o'zgaradi
 */
@Service
public class OtkazmaService {

    private final OtkazmaRepository otkazmaRepository;
    private final MahsulotRepository mahsulotRepository;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final TarixService tarixService;

    public OtkazmaService(OtkazmaRepository otkazmaRepository,
                          MahsulotRepository mahsulotRepository,
                          MagazinRepository magazinRepository,
                          UsersRepository usersRepository,
                          TarixService tarixService) {
        this.otkazmaRepository = otkazmaRepository;
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.tarixService = tarixService;
    }

    // ================= RO'YXATLAR =================

    /** Menga kelayotgan (tasdiq kutayotgan) o'tkazmalar */
    @Transactional(readOnly = true)
    public List<OtkazmaDto> kelayotganlar(String username) {
        Users u = user(username);
        if (u == null) return List.of();
        return owner(u)
                ? otkazmaRepository.findHolatBoyicha(OtkazmaHolati.KUTILMOQDA)
                : otkazmaRepository.findKelayotganlar(u.getId(), OtkazmaHolati.KUTILMOQDA);
    }

    /** Men jo'natgan o'tkazmalar (barcha holat) */
    @Transactional(readOnly = true)
    public List<OtkazmaDto> yuborganlarim(String username) {
        Users u = user(username);
        if (u == null) return List.of();
        return owner(u)
                ? otkazmaRepository.findHammasi()
                : otkazmaRepository.findMenYuborganlar(u.getId());
    }

    @Transactional(readOnly = true)
    public List<OtkazmaDto> mahsulotBoyicha(Long mahsulotId) {
        return otkazmaRepository.findMahsulotBoyicha(mahsulotId);
    }

    // ================= AMALLAR =================

    @Transactional
    public ApiResponse yuborish(String username, OtkazmaSaveDto dto) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);

        if (dto.mahsulotId() == null || dto.qayergaMagazinId() == null) {
            return new ApiResponse("Mahsulot va qabul qiluvchi magazin tanlanishi shart", false);
        }

        Mahsulot mahsulot = mahsulotRepository.findById(dto.mahsulotId()).orElse(null);
        if (mahsulot == null) return new ApiResponse("Mahsulot topilmadi", false);

        Magazin qayerdan = mahsulot.getMagazin();
        if (qayerdan == null) {
            return new ApiResponse("Mahsulot hech qaysi magazinga biriktirilmagan — " +
                    "avval unga magazin tanlang", false);
        }
        if (!mansubmi(u, qayerdan)) {
            return new ApiResponse("Bu mahsulot sizning magazinigizga tegishli emas", false);
        }

        Magazin qayerga = magazinRepository.findById(dto.qayergaMagazinId()).orElse(null);
        if (qayerga == null) return new ApiResponse("Qabul qiluvchi magazin topilmadi", false);
        if (Objects.equals(qayerga.getId(), qayerdan.getId())) {
            return new ApiResponse("Mahsulot allaqachon shu magazinda", false);
        }

        if (otkazmaRepository.existsByMahsulot_IdAndHolat(mahsulot.getId(), OtkazmaHolati.KUTILMOQDA)) {
            return new ApiResponse("Bu mahsulot bo'yicha tasdiq kutilayotgan o'tkazma bor", false);
        }

        double bor = mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor();
        if (bor <= 0) {
            return new ApiResponse("Mahsulot qoldig'i 0 — jo'natib bo'lmaydi", false);
        }
        // Miqdor kiritilmasa — butun qoldiq jo'natiladi
        double miqdor = dto.miqdor() == null ? bor : yaxlit(dto.miqdor());
        String miqdorXato = miqdorTekshir(miqdor, bor, mahsulot.getBirlik());
        if (miqdorXato != null) return new ApiResponse(miqdorXato, false);

        Otkazma o = new Otkazma();
        o.setMahsulot(mahsulot);
        o.setQayerdan(qayerdan);
        o.setQayerga(qayerga);
        o.setYuborgan(u);
        o.setHolat(OtkazmaHolati.KUTILMOQDA);
        o.setMiqdor(miqdor);
        o.setBirlik(mahsulot.getBirlik());
        o.setIzoh(dto.izoh());
        o.setYuborilganVaqt(LocalDateTime.now());
        otkazmaRepository.save(o);

        boolean toliq = toliqmi(miqdor, bor);
        tarixService.yoz("O'tkazma", "Jo'natildi", o.getId(), mahsulot.getNomi(),
                qayerdan.getNomi() + " -> " + qayerga.getNomi() +
                        " | Miqdor: " + son(miqdor) + " " + birlik(mahsulot.getBirlik()) +
                        (toliq ? " (to'liq)" : " (qismi, jami " + son(bor) + ")") +
                        (dto.izoh() == null || dto.izoh().isBlank() ? "" : " | Izoh: " + dto.izoh()));

        return new ApiResponse("\"" + qayerga.getNomi() + "\" magaziniga " +
                son(miqdor) + " " + birlik(mahsulot.getBirlik()) +
                " jo'natildi — tasdiq kutilmoqda", true);
    }

    /**
     * "Jo'natish"ning teskarisi: mahsulot boshqa magazinda turibdi, men (o'z magazinim
     * uchun) uni so'rayapman. Tasdiq/rad etish HOZIR mahsulot turgan (qayerdan) tomonga
     * tegishli — {@link #halQilishTekshir} shu yo'nalishni tekshiradi.
     */
    @Transactional
    public ApiResponse sorash(String username, OtkazmaSaveDto dto) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);

        if (dto.mahsulotId() == null || dto.qayergaMagazinId() == null) {
            return new ApiResponse("Mahsulot va o'z magaziningiz tanlanishi shart", false);
        }

        Mahsulot mahsulot = mahsulotRepository.findById(dto.mahsulotId()).orElse(null);
        if (mahsulot == null) return new ApiResponse("Mahsulot topilmadi", false);

        Magazin qayerga = magazinRepository.findById(dto.qayergaMagazinId()).orElse(null);
        if (qayerga == null) return new ApiResponse("Magazin topilmadi", false);
        if (!mansubmi(u, qayerga)) {
            return new ApiResponse("Siz bu magazinga mas'ul emassiz", false);
        }

        Magazin qayerdan = mahsulot.getMagazin();
        if (qayerdan == null) {
            return new ApiResponse("Bu mahsulot hech qaysi magazinga biriktirilmagan — so'rab bo'lmaydi", false);
        }
        if (Objects.equals(qayerdan.getId(), qayerga.getId())) {
            return new ApiResponse("Bu mahsulot allaqachon shu magazinda", false);
        }

        if (otkazmaRepository.existsByMahsulot_IdAndHolat(mahsulot.getId(), OtkazmaHolati.KUTILMOQDA)) {
            return new ApiResponse("Bu mahsulot bo'yicha tasdiq kutilayotgan o'tkazma bor", false);
        }

        double bor = mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor();
        if (bor <= 0) {
            return new ApiResponse("Mahsulot qoldig'i 0 — so'rab bo'lmaydi", false);
        }
        double miqdor = dto.miqdor() == null ? bor : yaxlit(dto.miqdor());
        String miqdorXato = miqdorTekshir(miqdor, bor, mahsulot.getBirlik());
        if (miqdorXato != null) return new ApiResponse(miqdorXato, false);

        Otkazma o = new Otkazma();
        o.setMahsulot(mahsulot);
        o.setQayerdan(qayerdan);
        o.setQayerga(qayerga);
        o.setYuborgan(u);
        o.setSorovmi(true);
        o.setHolat(OtkazmaHolati.KUTILMOQDA);
        o.setMiqdor(miqdor);
        o.setBirlik(mahsulot.getBirlik());
        o.setIzoh(dto.izoh());
        o.setYuborilganVaqt(LocalDateTime.now());
        otkazmaRepository.save(o);

        boolean toliq = toliqmi(miqdor, bor);
        tarixService.yoz("O'tkazma", "So'raldi", o.getId(), mahsulot.getNomi(),
                qayerga.getNomi() + " <- " + qayerdan.getNomi() +
                        " | Miqdor: " + son(miqdor) + " " + birlik(mahsulot.getBirlik()) +
                        (toliq ? " (to'liq)" : " (qismi, jami " + son(bor) + ")") +
                        (dto.izoh() == null || dto.izoh().isBlank() ? "" : " | Izoh: " + dto.izoh()));

        return new ApiResponse("So'rov \"" + qayerdan.getNomi() + "\" magaziniga tasdiq uchun yuborildi", true);
    }

    @Transactional
    public ApiResponse qabulQilish(String username, Long otkazmaId, String javobIzoh) {
        Users u = user(username);
        Otkazma o = otkazmaRepository.findById(otkazmaId).orElse(null);
        String xato = halQilishTekshir(u, o);
        if (xato != null) return new ApiResponse(xato, false);

        Mahsulot mahsulot = o.getMahsulot();
        Magazin eski = o.getQayerdan();

        double bor = mahsulot.getMiqdor() == null ? 0 : mahsulot.getMiqdor();
        double miqdor = o.getMiqdor() == null ? bor : o.getMiqdor();
        // Jo'natilgandan keyin qoldiq o'zgargan bo'lishi mumkin — qaytadan tekshiramiz
        String miqdorXato = miqdorTekshir(miqdor, bor, mahsulot.getBirlik());
        if (miqdorXato != null) {
            return new ApiResponse("Qabul qilib bo'lmadi: " + miqdorXato, false);
        }

        String natija;
        if (toliqmi(miqdor, bor)) {
            // To'liq jo'natilgan — mahsulotning o'zi ko'chadi
            mahsulot.setMagazin(o.getQayerga());
            mahsulotRepository.save(mahsulot);
            natija = "Mahsulot to'liq (" + son(miqdor) + " " + birlik(o.getBirlik()) + ") o'tdi";
        } else {
            // Qismi jo'natilgan (kesib berilgan) — yangi mahsulot ochiladi
            Mahsulot yangi = bolib(mahsulot, miqdor, o.getQayerga());
            mahsulotRepository.save(yangi);
            mahsulotRepository.save(mahsulot);
            o.setNatijaMahsulotId(yangi.getId());
            natija = son(miqdor) + " " + birlik(o.getBirlik()) + " kesib berildi — yangi kod: "
                    + yangi.getKod() + " (eski mahsulotda " + son(mahsulot.getMiqdor()) + " qoldi)";
        }

        o.setHolat(OtkazmaHolati.QABUL_QILINDI);
        o.setHalQilgan(u);
        o.setJavobIzoh(javobIzoh);
        o.setHalQilinganVaqt(LocalDateTime.now());
        otkazmaRepository.save(o);

        tarixService.yoz("O'tkazma", "Tasdiqlandi", o.getId(), mahsulot.getNomi(),
                (eski == null ? "—" : eski.getNomi()) + " -> " + o.getQayerga().getNomi() +
                        " | " + natija);
        tarixService.yoz("Mahsulot",
                o.getNatijaMahsulotId() == null ? "Magazini o'zgardi" : "Bo'lindi",
                mahsulot.getId(), mahsulot.getNomi(),
                "O'tkazma #" + o.getId() + " tasdiqlandi: " +
                        (eski == null ? "—" : eski.getNomi()) + " -> " + o.getQayerga().getNomi() +
                        " | " + natija);

        return new ApiResponse("O'tkazma tasdiqlandi — " + natija, true);
    }

    @Transactional
    public ApiResponse radEtish(String username, Long otkazmaId, String sabab) {
        Users u = user(username);
        Otkazma o = otkazmaRepository.findById(otkazmaId).orElse(null);
        String xato = halQilishTekshir(u, o);
        if (xato != null) return new ApiResponse(xato, false);

        o.setHolat(OtkazmaHolati.RAD_ETILDI);
        o.setHalQilgan(u);
        o.setJavobIzoh(sabab);
        o.setHalQilinganVaqt(LocalDateTime.now());
        otkazmaRepository.save(o);

        tarixService.yoz("O'tkazma", "Rad etildi", o.getId(), o.getMahsulot().getNomi(),
                (o.getQayerdan() == null ? "—" : o.getQayerdan().getNomi()) + " -> " +
                        o.getQayerga().getNomi() +
                        (sabab == null || sabab.isBlank() ? "" : " | Sabab: " + sabab));

        return new ApiResponse("O'tkazma rad etildi — mahsulot eski magazinda qoldi", true);
    }

    /** Jo'natgan odam tasdiqlanmagunicha bekor qila oladi */
    @Transactional
    public ApiResponse bekorQilish(String username, Long otkazmaId) {
        Users u = user(username);
        if (u == null) return new ApiResponse("Foydalanuvchi topilmadi", false);

        Otkazma o = otkazmaRepository.findById(otkazmaId).orElse(null);
        if (o == null) return new ApiResponse("O'tkazma topilmadi", false);
        if (o.getHolat() != OtkazmaHolati.KUTILMOQDA) {
            return new ApiResponse("Bu o'tkazma allaqachon hal qilingan", false);
        }
        boolean ozi = o.getYuborgan() != null && Objects.equals(o.getYuborgan().getId(), u.getId());
        if (!ozi && !owner(u)) {
            return new ApiResponse("Faqat jo'natgan hodim bekor qila oladi", false);
        }

        o.setHolat(OtkazmaHolati.BEKOR_QILINDI);
        o.setHalQilgan(u);
        o.setHalQilinganVaqt(LocalDateTime.now());
        otkazmaRepository.save(o);

        tarixService.yoz("O'tkazma", "Bekor qilindi", o.getId(), o.getMahsulot().getNomi(),
                "Jo'natuvchi tomonidan bekor qilindi");

        return new ApiResponse("O'tkazma bekor qilindi", true);
    }

    // ================= YORDAMCHI =================

    private String halQilishTekshir(Users u, Otkazma o) {
        if (u == null) return "Foydalanuvchi topilmadi";
        if (o == null) return "O'tkazma topilmadi";
        if (o.getHolat() != OtkazmaHolati.KUTILMOQDA) {
            return "Bu o'tkazma allaqachon hal qilingan: " + o.getHolat().getNomi();
        }
        // So'rovda tasdiq/rad HOZIR mahsulot turgan (qayerdan) tomonga tegishli —
        // oddiy jo'natishda esa qabul qiluvchi (qayerga) tomonga
        Magazin halQiluvchi = o.isSorovmi() ? o.getQayerdan() : o.getQayerga();
        if (!mansubmi(u, halQiluvchi)) {
            return o.isSorovmi()
                    ? "Bu so'rov sizning magazinigizga tegishli emas"
                    : "Bu o'tkazma sizning magazinigizga kelmagan";
        }
        return null;
    }

    /** Miqdor to'g'rimi: 0 dan katta, qoldiqdan oshmaydi, dona uchun butun */
    private String miqdorTekshir(double miqdor, double bor, String birlik) {
        if (miqdor <= 0) {
            return "Miqdor 0 dan katta bo'lishi kerak";
        }
        if (miqdor > bor + 0.0001) {
            return "Mahsulotda faqat " + son(bor) + " " + birlik(birlik) + " bor";
        }
        if (MahsulotService.donami(birlik)
                && Math.abs(miqdor - Math.round(miqdor)) > 0.0001) {
            return "Dona uchun miqdor butun son bo'lishi kerak";
        }
        return null;
    }

    private boolean toliqmi(double miqdor, double bor) {
        return miqdor >= bor - 0.0001;
    }

    /**
     * Mahsulotni bo'ladi: `miqdor` qismi yangi magazinga yangi mahsulot bo'lib chiqadi,
     * eski mahsulotda qoldig'i qoladi. Narx va o'lchamlar nisbatan qayta hisoblanadi.
     */
    private Mahsulot bolib(Mahsulot manba, double miqdor, Magazin qayerga) {
        double bor = manba.getMiqdor() == null ? 0 : manba.getMiqdor();
        double ulush = bor > 0 ? miqdor / bor : 1;

        Mahsulot yangi = new Mahsulot();
        yangi.setNomi(manba.getNomi());
        yangi.setKod(bosKod(manba.getKod()));
        yangi.setKategoriya(manba.getKategoriya());
        yangi.setBirlik(manba.getBirlik());
        yangi.setTuri(manba.getTuri());
        yangi.setMagazin(qayerga);
        yangi.setYaratganUser(manba.getYaratganUser());
        yangi.setMiqdor(yaxlit(miqdor));

        // O'lchamlar: eni saqlanadi, bo'yi kesilgan miqdorga qarab qayta hisoblanadi
        yangi.setEni(manba.getEni());
        if (MahsulotService.donami(manba.getBirlik())) {
            yangi.setBoyi(manba.getBoyi());   // dona — o'lcham o'zgarmaydi
        } else if ("metr".equals(manba.getBirlik())) {
            yangi.setBoyi(yaxlit(miqdor));
            manba.setBoyi(yaxlit(bor - miqdor));
        } else { // kv.metr
            Double eni = manba.getEni();
            if (eni != null && eni > 0) {
                yangi.setBoyi(yaxlit(miqdor / eni));
                manba.setBoyi(yaxlit((bor - miqdor) / eni));
            } else {
                yangi.setBoyi(manba.getBoyi());
            }
        }
        yangi.setKv(kv(yangi.getBoyi(), yangi.getEni()));

        // Narx miqdorga proporsional bo'linadi
        Long narx = manba.getZavodNarxi();
        if (narx != null) {
            long yangiNarx = Math.round(narx * ulush);
            yangi.setZavodNarxi(yangiNarx);
            manba.setZavodNarxi(narx - yangiNarx);
        }

        manba.setMiqdor(yaxlit(bor - miqdor));
        manba.setKv(kv(manba.getBoyi(), manba.getEni()));
        return yangi;
    }

    /** Kesilgan bo'lakka bo'sh kod topadi: GL-1024 -> GL-1024-1, GL-1024-2 ... */
    private String bosKod(String asosKod) {
        String asos = asosKod == null || asosKod.isBlank() ? "MH" : asosKod.trim();
        for (int i = 1; i < 1000; i++) {
            String kod = asos + "-" + i;
            if (mahsulotRepository.findByKodIgnoreCase(kod).isEmpty()) {
                return kod;
            }
        }
        return asos + "-" + System.currentTimeMillis();
    }

    private Double kv(Double boyi, Double eni) {
        if (boyi == null || eni == null) return null;
        return Math.round(boyi * eni * 100.0) / 100.0;
    }

    private Double yaxlit(Double son) {
        return son == null ? null : Math.round(son * 100.0) / 100.0;
    }

    private double yaxlit(double son) {
        return Math.round(son * 100.0) / 100.0;
    }

    /** Chiroyli chiqarish uchun: 12.0 -> "12",  12.5 -> "12.5" */
    private String son(double son) {
        return son == Math.rint(son)
                ? String.valueOf((long) son)
                : String.valueOf(Math.round(son * 100.0) / 100.0);
    }

    private String birlik(String b) {
        return b == null || b.isBlank() ? "dona" : b;
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
}