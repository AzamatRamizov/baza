package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Repository.KategoriyaRepository;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MahsulotService {

    private final MahsulotRepository mahsulotRepository;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final KategoriyaRepository kategoriyaRepository;
    private final ValyutaService valyutaService;
    private final TarixService tarixService;

    public MahsulotService(MahsulotRepository mahsulotRepository,
                           MagazinRepository magazinRepository,
                           UsersRepository usersRepository,
                           KategoriyaRepository kategoriyaRepository,
                           ValyutaService valyutaService,
                           TarixService tarixService) {
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.kategoriyaRepository = kategoriyaRepository;
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
        String xato = tekshir(dto, null);
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

        String xato = tekshir(dto, id);
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

    /** Tarix uchun mahsulotning qisqacha holati */
    private String mahsulotHolati(Mahsulot m) {
        return "kod=" + m.getKod()
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

    private String tekshir(MahsulotSaveDto dto, Long ozId) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Mahsulot nomi kiritilishi shart";
        }
        if (dto.kod() == null || dto.kod().isBlank()) {
            return "Mahsulot kodi kiritilishi shart";
        }
        Optional<Mahsulot> kodMavjud = mahsulotRepository.findByKodIgnoreCase(dto.kod().trim());
        if (kodMavjud.isPresent() && !Objects.equals(kodMavjud.get().getId(), ozId)) {
            return "Bu kod bilan mahsulot allaqachon mavjud: " + kodMavjud.get().getNomi();
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