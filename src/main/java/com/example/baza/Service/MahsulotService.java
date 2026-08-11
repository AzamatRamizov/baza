package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.MahsulotQidirDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.SotuvHolati;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.KategoriyaRepository;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.SotuvRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class MahsulotService {

    private final MahsulotRepository mahsulotRepository;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final KategoriyaRepository kategoriyaRepository;
    private final SotuvRepository sotuvRepository;
    private final ValyutaService valyutaService;
    private final TarixService tarixService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private static final Set<String> RUXSAT_ETILGAN_KENGAYTMALAR = Set.of("jpg", "jpeg", "png", "webp");

    /** Zaxiradan haqiqatan chiqib ketgan holatlar — "sotilgan summa" shular bo'yicha hisoblanadi */
    private static final List<SotuvHolati> SOTILGAN_HOLATLAR = List.of(SotuvHolati.SOTILDI, SotuvHolati.KATMDA);

    public MahsulotService(MahsulotRepository mahsulotRepository,
                           MagazinRepository magazinRepository,
                           UsersRepository usersRepository,
                           KategoriyaRepository kategoriyaRepository,
                           SotuvRepository sotuvRepository,
                           ValyutaService valyutaService,
                           TarixService tarixService) {
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.kategoriyaRepository = kategoriyaRepository;
        this.sotuvRepository = sotuvRepository;
        this.valyutaService = valyutaService;
        this.tarixService = tarixService;
    }

    public List<MahsulotDto> getAllMahsulotlar() {
        return mahsulotRepository.findAllDto();
    }

    /**
     * "Mening mahsulotlarim" — men mas'ul bo'lgan magazin(lar)dagi mahsulotlar.
     * Bitta hodim bir nechta magazinga mas'ul bo'lishi mumkin.
     */
    public List<MahsulotDto> getMeningMahsulotlarim(String username) {
        return usersRepository.findByUsername(username)
                .map(u -> mahsulotRepository.findMagazinimDto(u.getId()))
                .orElseGet(List::of);
    }

    @Transactional
    public ApiResponse addMahsulot(MahsulotSaveDto dto) {
        String xato = tekshir(dto, null, null, null);
        if (xato != null) return new ApiResponse(xato, false);

        Double miqdor = miqdorniAniqla(dto);
        Long narxSom = narxniSomgaAylantir(dto, miqdor);
        if (narxSom == null && dto.zavodNarxi() != null) {
            return new ApiResponse(
                    "Dollar kursini olib bo'lmadi — keyinroq urinib ko'ring yoki narxni so'mda kiriting", false);
        }

        Mahsulot mahsulot = new Mahsulot();
        maydonlarniTuldirish(mahsulot, dto, narxSom, miqdor);

        // Yaratgan user — SecurityContext'dan (faqat yaratishda)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        usersRepository.findByUsername(username).ifPresent(mahsulot::setYaratganUser);

        mahsulotRepository.save(mahsulot);

        tarixService.yoz("Mahsulot", "Qo'shildi", mahsulot.getId(), mahsulot.getNomi(),
                "Kod: " + mahsulot.getKod() +
                        " | Magazin: " + (mahsulot.getMagazin() == null ? "—" : mahsulot.getMagazin().getNomi()) +
                        " | Miqdor: " + mahsulot.getMiqdor() + " " + mahsulot.getBirlik() +
                        " | Zavod narxi: " + (mahsulot.getZavodNarxi() == null ? "—" : mahsulot.getZavodNarxi() + " so'm"));
        return new ApiResponse("Mahsulot qo'shildi", true);
    }

    @Transactional
    public ApiResponse updateMahsulot(Long id, MahsulotSaveDto dto) {
        Mahsulot mahsulot = mahsulotRepository.findById(id).orElse(null);
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot topilmadi", false);
        }

        String xato = tekshir(dto, id, mahsulot.getKod(), mahsulot.getMaxsusKod());
        if (xato != null) return new ApiResponse(xato, false);

        Double miqdor = miqdorniAniqla(dto);
        Long narxSom = narxniSomgaAylantir(dto, miqdor);
        if (narxSom == null && dto.zavodNarxi() != null) {
            return new ApiResponse(
                    "Dollar kursini olib bo'lmadi — keyinroq urinib ko'ring yoki narxni so'mda kiriting", false);
        }

        String eskiHolat = mahsulotHolati(mahsulot);
        maydonlarniTuldirish(mahsulot, dto, narxSom, miqdor); // yaratganUser o'zgarmaydi
        mahsulotRepository.save(mahsulot);

        String yangiHolat = mahsulotHolati(mahsulot);
        tarixService.yoz("Mahsulot", "Tahrirlandi", mahsulot.getId(), mahsulot.getNomi(),
                eskiHolat.equals(yangiHolat) ? null : eskiHolat + "  =>  " + yangiHolat);
        return new ApiResponse("Mahsulot yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteMahsulot(Long id) {
        Mahsulot mahsulot = mahsulotRepository.findById(id).orElse(null);
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot topilmadi", false);
        }
        String nomi = mahsulot.getNomi();
        String kod = mahsulot.getKod();
        mahsulotRepository.delete(mahsulot);

        tarixService.yoz("Mahsulot", "O'chirildi", id, nomi, "Kod: " + kod);
        return new ApiResponse("Mahsulot o'chirildi", true);
    }

    /**
     * Detail sahifasi uchun — "tegishli" va "sotilganSumma" maydonlari joriy
     * mahsulotga nisbatan hisoblab qo'yiladi (ro'yxat so'rovlarida hisoblanmaydi).
     */
    public Optional<MahsulotDto> getMahsulotDto(Long id, String username) {
        return mahsulotRepository.findDtoById(id)
                .map(dto -> dto.detailUchunTuldirish(
                        tegishlimi(id, username),
                        sotuvRepository.sumSummaMahsulotBoyicha(id, SOTILGAN_HOLATLAR)));
    }

    /**
     * Shtrix-kod/QR skaneri kodni o'qiganda chaqiriladi.
     * Avval TAKRORLANMAS "maxsus kod" bo'yicha aniq moslikni qidiradi (yagona natija,
     * noaniqlik yo'q); topilmasa — orqaga qarab oddiy "kod" (artikul) bo'yicha qidiradi,
     * bu esa bir necha mahsulotga tegishli bo'lishi mumkin (ommaviy import ataylab
     * takrorlaydi, {@link MahsulotRepository#findByKodIgnoreCase} izohiga qarang) —
     * shunday holatda "koplik" true qaytadi, chaqiruvchi ro'yxatga o'tkazadi.
     * <p>
     * Natijaga skanerlagan hodimga mahsulot tegishlimi (uning magazin(lar)i) qo'shiladi —
     * chaqiruvchi tomon "tegishli emas" bo'lsa ogohlantirish ko'rsatib, o'tishni to'xtatadi.
     */
    public Optional<MahsulotQidirDto> kodBoyichaQidir(String kod, String username) {
        if (kod == null || kod.isBlank()) return Optional.empty();
        String tozaKod = kod.trim();

        Optional<Mahsulot> maxsusTopilgan = mahsulotRepository.findByMaxsusKodIgnoreCase(tozaKod);
        if (maxsusTopilgan.isPresent()) {
            return Optional.of(qidirDtoYasash(maxsusTopilgan.get(), tozaKod, username));
        }

        List<Mahsulot> topilganlar = mahsulotRepository.findByKodIgnoreCase(tozaKod);
        if (topilganlar.isEmpty()) return Optional.empty();
        if (topilganlar.size() > 1) {
            // Ko'plik holatida tegishlilik hisoblanmaydi — ro'yxatga o'tkaziladi, u yerda ko'rinadi
            return Optional.of(new MahsulotQidirDto(null, true, tozaKod, true, null, null));
        }
        return Optional.of(qidirDtoYasash(topilganlar.get(0), tozaKod, username));
    }

    private MahsulotQidirDto qidirDtoYasash(Mahsulot mahsulot, String kod, String username) {
        boolean tegishli = tegishlimi(mahsulot.getId(), username);
        String magazinNomi = mahsulot.getMagazin() == null ? null : mahsulot.getMagazin().getNomi();
        return new MahsulotQidirDto(mahsulot.getId(), false, kod, tegishli, mahsulot.getNomi(), magazinNomi);
    }

    /** Owner har doim "tegishli" — hodim uchun mahsulotning magazini o'ziga biriktirilgan bo'lishi shart */
    private boolean tegishlimi(Long mahsulotId, String username) {
        Users u = usersRepository.findByUsername(username).orElse(null);
        if (u == null) return false;
        boolean owner = u.getRollar() != null && u.getRollar().stream().anyMatch(Rol::isTizimRoli);
        if (owner) return true;
        return mahsulotRepository.existsByIdAndMagazin_Hodimlar_Id(mahsulotId, u.getId());
    }

    /**
     * Mahsulot rasmini yuklash — diskka {app.upload-dir}/mahsulot/ ostiga saqlaydi,
     * eski rasm bo'lsa uni o'chiradi. DBda faqat fayl nomi saqlanadi.
     */
    @Transactional
    public ApiResponse rasmniYuklash(Long id, MultipartFile file) {
        Mahsulot mahsulot = mahsulotRepository.findById(id).orElse(null);
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot topilmadi", false);
        }
        if (file == null || file.isEmpty()) {
            return new ApiResponse("Rasm tanlanmagan", false);
        }

        String kengaytma = kengaytmaniOl(file.getOriginalFilename());
        if (kengaytma == null || !RUXSAT_ETILGAN_KENGAYTMALAR.contains(kengaytma)) {
            return new ApiResponse("Faqat JPG, PNG yoki WEBP formatdagi rasm yuklash mumkin", false);
        }

        try {
            Path papka = Path.of(uploadDir, "mahsulot");
            Files.createDirectories(papka);

            String yangiNom = UUID.randomUUID() + "." + kengaytma;
            Files.copy(file.getInputStream(), papka.resolve(yangiNom), StandardCopyOption.REPLACE_EXISTING);

            String eskiRasm = mahsulot.getRasm();
            mahsulot.setRasm(yangiNom);
            mahsulotRepository.save(mahsulot);
            rasmniDiskdanOchirish(eskiRasm);

            tarixService.yoz("Mahsulot", "Rasm yuklandi", mahsulot.getId(), mahsulot.getNomi(), null);
            return new ApiResponse("Rasm yuklandi", true);
        } catch (IOException e) {
            throw new UncheckedIOException("Rasmni saqlab bo'lmadi", e);
        }
    }

    @Transactional
    public ApiResponse rasmniOchirish(Long id) {
        Mahsulot mahsulot = mahsulotRepository.findById(id).orElse(null);
        if (mahsulot == null) {
            return new ApiResponse("Mahsulot topilmadi", false);
        }
        String eskiRasm = mahsulot.getRasm();
        if (eskiRasm == null) {
            return new ApiResponse("Mahsulotda rasm yo'q", false);
        }
        mahsulot.setRasm(null);
        mahsulotRepository.save(mahsulot);
        rasmniDiskdanOchirish(eskiRasm);

        tarixService.yoz("Mahsulot", "Rasm o'chirildi", mahsulot.getId(), mahsulot.getNomi(), null);
        return new ApiResponse("Rasm o'chirildi", true);
    }

    private void rasmniDiskdanOchirish(String rasmNomi) {
        if (rasmNomi == null) return;
        try {
            Files.deleteIfExists(Path.of(uploadDir, "mahsulot", rasmNomi));
        } catch (IOException ignored) {
            // eski rasmni o'chirib bo'lmasa ham davom etamiz — yangi rasm baribir saqlangan
        }
    }

    private String kengaytmaniOl(String faylNomi) {
        if (faylNomi == null) return null;
        int nuqta = faylNomi.lastIndexOf('.');
        if (nuqta < 0 || nuqta == faylNomi.length() - 1) return null;
        return faylNomi.substring(nuqta + 1).toLowerCase();
    }

    /** Tarix uchun mahsulotning qisqacha holati */
    private String mahsulotHolati(Mahsulot m) {
        return "kod=" + m.getKod()
                + ", maxsusKod=" + (m.getMaxsusKod() == null ? "—" : m.getMaxsusKod())
                + ", nomi=" + m.getNomi()
                + ", kategoriya=" + (m.getKategoriya() == null ? "—" : m.getKategoriya().getNomi())
                + ", turi=" + m.getTuri()
                + ", birlik=" + m.getBirlik()
                + ", o'lcham=" + m.getBoyi() + "x" + m.getEni()
                + ", kv=" + m.getKv()
                + ", miqdor=" + m.getMiqdor() + " " + m.getBirlik()
                + ", narx=" + m.getZavodNarxi()
                + ", magazin=" + (m.getMagazin() == null ? "—" : m.getMagazin().getNomi());
    }

    // ================= YORDAMCHI =================

    /**
     * @param ozId    tahrirlanayotgan mahsulot id'si (yangi qo'shishda null)
     * @param eskiKod tahrirlanayotgan mahsulotning HOZIRGI kodi (yangi qo'shishda null) —
     *                agar dto.kod() shu bilan bir xil bo'lsa (kod o'zgarmagan), takrorlanish
     *                tekshiruvi o'tkazib yuboriladi. Bu — ommaviy import bir xil kodni
     *                bir necha marta qo'shishi mumkinligi uchun kerak: aks holda allaqachon
     *                takrorlangan kod bilan mahsulotni (hech narsa o'zgartirmasdan ham)
     *                saqlab bo'lmay qoladi.
     * @param eskiMaxsusKod tahrirlanayotgan mahsulotning HOZIRGI maxsus kodi — xuddi
     *                eskiKod kabi, o'zgarmagan bo'lsa takrorlanish tekshiruvi o'tkazib yuboriladi.
     */
    private String tekshir(MahsulotSaveDto dto, Long ozId, String eskiKod, String eskiMaxsusKod) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Mahsulot nomi kiritilishi shart";
        }
        if (dto.kod() == null || dto.kod().isBlank()) {
            return "Mahsulot kodi kiritilishi shart";
        }
        String yangiKod = dto.kod().trim();
        boolean kodOzgardi = eskiKod == null || !yangiKod.equalsIgnoreCase(eskiKod);
        if (kodOzgardi) {
            Mahsulot boshqasi = mahsulotRepository.findByKodIgnoreCase(yangiKod).stream()
                    .filter(m -> !Objects.equals(m.getId(), ozId))
                    .findFirst().orElse(null);
            if (boshqasi != null) {
                return "Bu kod bilan mahsulot allaqachon mavjud: " + boshqasi.getNomi();
            }
        }
        // Maxsus kod (skaner uchun) — ixtiyoriy, lekin kiritilsa TAKRORLANMAS bo'lishi shart
        if (dto.maxsusKod() != null && !dto.maxsusKod().isBlank()) {
            String yangiMaxsusKod = dto.maxsusKod().trim();
            boolean maxsusKodOzgardi = eskiMaxsusKod == null || !yangiMaxsusKod.equalsIgnoreCase(eskiMaxsusKod);
            if (maxsusKodOzgardi) {
                Mahsulot boshqasi = mahsulotRepository.findByMaxsusKodIgnoreCase(yangiMaxsusKod)
                        .filter(m -> !Objects.equals(m.getId(), ozId))
                        .orElse(null);
                if (boshqasi != null) {
                    return "Bu maxsus kod boshqa mahsulotda band: " + boshqasi.getNomi();
                }
            }
        }
        if (dto.turi() == null || dto.turi().isBlank()) {
            return "Mahsulot turi tanlanishi shart";
        }

        // Narx HAR DOIM 1 kv.metr uchun olinadi -> o'lchamlar majburiy
        if (dto.boyi() == null || dto.boyi() <= 0 || dto.eni() == null || dto.eni() <= 0) {
            return "Bo'yi va enini kiriting \u2014 narx 1 kv.metr uchun hisoblanadi";
        }

        if (gatoviymi(dto.turi())) {
            // Gatoviy — dona bilan sanaladi, nechtaligi majburiy
            if (dto.miqdor() == null || dto.miqdor() <= 0) {
                return "Gatoviy mahsulot uchun nechtaligini kiriting";
            }
            if (Math.abs(dto.miqdor() - Math.round(dto.miqdor())) > 0.0001) {
                return "Dona uchun miqdor butun son bo'lishi kerak";
            }
        }
        if (dto.zavodNarxi() != null && dto.zavodNarxi() < 0) {
            return "Zavod narxi manfiy bo'lishi mumkin emas";
        }
        if ((dto.boyi() != null && dto.boyi() < 0) || (dto.eni() != null && dto.eni() < 0)) {
            return "O'lchamlar manfiy bo'lishi mumkin emas";
        }
        if (dto.miqdor() != null && dto.miqdor() <= 0) {
            return "Miqdor 0 dan katta bo'lishi kerak";
        }
        return null;
    }

    /**
     * Zavod narxi HAR DOIM 1 KV.METR uchun kiritiladi — dona soniga bog'liq emas.
     * Umumiy narx jami kvadratga ko'paytiriladi:
     *   umumiy = jamiKv * (1 kv.metr narxi)
     * Masalan:
     *   gatoviy, 3x4 (12 kv), 5 dona, 3.3$/kv -> 5*12 = 60 kv -> 60 * 3.3 = 198$
     *   metraj,  3x100 (300 kv),      3.3$/kv -> 300 * 3.3 = 990$
     * - valyuta "USD" bo'lsa — CBU kursi bo'yicha so'mga (kurs topilmasa null),
     * - so'm bo'lsa — so'mligicha.
     */
    private Long narxniSomgaAylantir(MahsulotSaveDto dto, Double miqdor) {
        if (dto.zavodNarxi() == null) return null;

        double narx = dto.zavodNarxi();

        if ("USD".equals(dto.valyuta())) {
            UsdKursDto kurs = valyutaService.getUsdKurs();
            if (kurs.kurs() == null) return null; // kurs olinmadi
            narx *= kurs.kurs();
        }

        Double kv = jamiKv(dto, miqdor);
        if (kv != null && kv > 0) {
            narx *= kv;      // narx 1 kv.metr uchun -> umumiy = jami kv × narx
        }

        return Math.round(narx);
    }

    /**
     * Mahsulotning JAMI kvadrati (narx shu bo'yicha hisoblanadi):
     *   gatoviy -> dona soni * (bo'yi * eni)     (5 ta 3x4 gilam = 60 kv)
     *   metraj  -> miqdorning o'zi (u allaqachon kv.metrda)
     */
    static Double jamiKv(MahsulotSaveDto dto, Double miqdor) {
        Double birKv = (dto.boyi() == null || dto.eni() == null)
                ? null : yaxlit(dto.boyi() * dto.eni());

        if (!gatoviymi(dto.turi())) {
            return miqdor != null && miqdor > 0 ? miqdor : birKv;
        }
        if (birKv == null || birKv <= 0) return null;

        double dona = miqdor == null || miqdor <= 0 ? 1 : miqdor;
        return yaxlit(birKv * dona);
    }

    /**
     * Miqdor kiritilmagan bo'lsa birlikdan kelib chiqib o'zi hisoblanadi:
     *   kv.metr -> kv (boyi * eni),  metr -> bo'yi,  dona -> 1
     */
    private Double miqdorniAniqla(MahsulotSaveDto dto) {
        String birlik = birlikniAniqla(dto.turi(), dto.eni());

        // Gatoviy — foydalanuvchi kiritgan dona soni
        if (donami(birlik)) {
            return dto.miqdor() != null && dto.miqdor() > 0
                    ? yaxlit((double) Math.round(dto.miqdor()))
                    : 1.0;
        }

        // Metraj — HAR DOIM o'lchamlardan hisoblanadi (qo'lda kiritilmaydi)
        if ("kv.metr".equals(birlik)) {
            Double kv = kvHisobla(dto.boyi(), dto.eni());
            if (kv != null && kv > 0) return kv;
        }
        if (dto.boyi() != null && dto.boyi() > 0) {
            return yaxlit(dto.boyi());
        }
        return dto.miqdor() != null && dto.miqdor() > 0 ? yaxlit(dto.miqdor()) : 1.0;
    }

    /**
     * Birlik TURI dan kelib chiqadi (formada alohida so'ralmaydi):
     *   gatoviy -> dona
     *   metraj  -> kv.metr (eni bo'lsa) / metr (eni bo'lmasa)
     */
    static String birlikniAniqla(String turi, Double eni) {
        if (gatoviymi(turi)) return "dona";
        return (eni != null && eni > 0) ? "kv.metr" : "metr";
    }

    static boolean gatoviymi(String turi) {
        return turi == null || turi.isBlank() || "gatoviy".equalsIgnoreCase(turi.trim());
    }

    static boolean donami(String birlik) {
        return birlik == null || "dona".equals(birlik);
    }

    static Double yaxlit(Double son) {
        return son == null ? null : Math.round(son * 100.0) / 100.0;
    }

    private void maydonlarniTuldirish(Mahsulot mahsulot, MahsulotSaveDto dto,
                                      Long narxSom, Double miqdor) {
        mahsulot.setNomi(dto.nomi().trim());
        mahsulot.setKod(dto.kod().trim());
        mahsulot.setMaxsusKod(dto.maxsusKod() == null || dto.maxsusKod().isBlank()
                ? null : dto.maxsusKod().trim());
        mahsulot.setKategoriya(dto.kategoriyaId() == null
                ? null
                : kategoriyaRepository.findById(dto.kategoriyaId()).orElse(null));
        mahsulot.setBirlik(birlikniAniqla(dto.turi(), dto.eni()));
        mahsulot.setZavodNarxi(narxSom);
        mahsulot.setBoyi(dto.boyi());
        mahsulot.setEni(dto.eni());
        mahsulot.setKv(kvHisobla(dto.boyi(), dto.eni()));
        mahsulot.setMiqdor(miqdor);
        mahsulot.setTuri(dto.turi());

        if (dto.magazinId() != null) {
            mahsulot.setMagazin(magazinRepository.findById(dto.magazinId()).orElse(null));
        } else {
            mahsulot.setMagazin(null);
        }
    }

    /** Kv avtomatik: boyi * eni (ikkala o'lcham ham bo'lsa), 2 xonagacha yaxlitlanadi */
    private Double kvHisobla(Double boyi, Double eni) {
        if (boyi == null || eni == null) return null;
        return Math.round(boyi * eni * 100.0) / 100.0;
    }
}