package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KategoriyaDto;
import com.example.baza.Dto.ImportNatijaDto;
import com.example.baza.Dto.KategoriyaSaveDto;
import com.example.baza.Entity.Kategoriya;
import com.example.baza.Repository.KategoriyaRepository;
import com.example.baza.Repository.MahsulotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class KategoriyaService {

    private final KategoriyaRepository kategoriyaRepository;
    private final MahsulotRepository mahsulotRepository;
    private final TarixService tarixService;
    private final ExcelOqishService excelOqishService;

    /** Bitta faylda ruxsat etilgan maksimal nomlar soni */
    private static final int MAX_QATOR = 5000;

    public KategoriyaService(KategoriyaRepository kategoriyaRepository,
                             MahsulotRepository mahsulotRepository,
                             TarixService tarixService,
                             ExcelOqishService excelOqishService) {
        this.kategoriyaRepository = kategoriyaRepository;
        this.mahsulotRepository = mahsulotRepository;
        this.tarixService = tarixService;
        this.excelOqishService = excelOqishService;
    }

    public List<KategoriyaDto> getAllKategoriyalar() {
        return kategoriyaRepository.findAllByOrderByNomiAsc().stream()
                .map(k -> new KategoriyaDto(k.getId(), k.getNomi(), k.getNarxi(), k.getValyuta()))
                .toList();
    }

    @Transactional
    public ApiResponse addKategoriya(KategoriyaSaveDto dto) {
        String xato = nomiTekshir(dto, null);
        if (xato != null) return new ApiResponse(xato, false);

        Kategoriya kategoriya = new Kategoriya();
        kategoriya.setNomi(dto.nomi().trim());
        kategoriya.setNarxi(dto.narxi());
        kategoriya.setValyuta(valyutaAniqla(dto));
        kategoriyaRepository.save(kategoriya);

        tarixService.yoz("Kategoriya", "Qo'shildi",
                kategoriya.getId(), kategoriya.getNomi(),
                dto.narxi() == null ? null : "Narxi: " + dto.narxi() + " " + kategoriya.getValyuta() + "/kv.metr");
        return new ApiResponse("Kategoriya qo'shildi", true);
    }

    @Transactional
    public ApiResponse updateKategoriya(Long id, KategoriyaSaveDto dto) {
        Kategoriya kategoriya = kategoriyaRepository.findById(id).orElse(null);
        if (kategoriya == null) {
            return new ApiResponse("Kategoriya topilmadi", false);
        }

        String xato = nomiTekshir(dto, id);
        if (xato != null) return new ApiResponse(xato, false);

        String eskiNomi = kategoriya.getNomi();
        Double eskiNarxi = kategoriya.getNarxi();
        String eskiValyuta = kategoriya.getValyuta();
        kategoriya.setNomi(dto.nomi().trim());
        kategoriya.setNarxi(dto.narxi());
        kategoriya.setValyuta(valyutaAniqla(dto));
        kategoriyaRepository.save(kategoriya);

        StringBuilder tafsilot = new StringBuilder();
        if (!eskiNomi.equals(kategoriya.getNomi())) {
            tafsilot.append("Nomi: ").append(eskiNomi).append(" -> ").append(kategoriya.getNomi());
        }
        if (!Objects.equals(eskiNarxi, kategoriya.getNarxi()) || !Objects.equals(eskiValyuta, kategoriya.getValyuta())) {
            if (tafsilot.length() > 0) tafsilot.append(" | ");
            tafsilot.append("Narxi: ").append(eskiNarxi).append(" ").append(eskiValyuta)
                    .append(" -> ").append(kategoriya.getNarxi()).append(" ").append(kategoriya.getValyuta());
        }
        tarixService.yoz("Kategoriya", "Tahrirlandi",
                kategoriya.getId(), kategoriya.getNomi(),
                tafsilot.length() == 0 ? null : tafsilot.toString());
        return new ApiResponse("Kategoriya yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteKategoriya(Long id) {
        Kategoriya kategoriya = kategoriyaRepository.findById(id).orElse(null);
        if (kategoriya == null) {
            return new ApiResponse("Kategoriya topilmadi", false);
        }

        long mahsulotSoni = mahsulotRepository.countByKategoriya_Id(id);
        if (mahsulotSoni > 0) {
            return new ApiResponse(
                    "Bu kategoriyada " + mahsulotSoni + " ta mahsulot bor — " +
                            "avval ularni boshqa kategoriyaga o'tkazing", false);
        }

        kategoriyaRepository.deleteById(id);

        tarixService.yoz("Kategoriya", "O'chirildi", id, kategoriya.getNomi(), null);
        return new ApiResponse("Kategoriya o'chirildi", true);
    }

    /**
     * Exceldan (yoki CSV'dan) kategoriyalarni ommaviy qo'shish.
     * Faylda faqat nomlar bo'ladi — har qatordan birinchi to'ldirilgan katak olinadi.
     *
     * Bazada bor nomlar va fayl ichidagi takrorlar o'tkazib yuboriladi
     * (xato deb hisoblanmaydi) — shuning uchun bitta faylni ikki marta
     * yuklash ham xavfsiz.
     */
    @Transactional
    public ImportNatijaDto importQil(MultipartFile file) {
        List<String> nomlar;
        try {
            nomlar = excelOqishService.nomlarniOqi(file);
        } catch (IllegalArgumentException e) {
            return ImportNatijaDto.xato(e.getMessage());
        }

        if (nomlar.isEmpty()) {
            return ImportNatijaDto.xato("Faylda birorta ham nom topilmadi");
        }
        if (nomlar.size() > MAX_QATOR) {
            return ImportNatijaDto.xato(
                    "Faylda juda ko'p qator (" + nomlar.size() + "). " +
                            "Bir martada eng ko'pi " + MAX_QATOR + " ta bo'lishi mumkin");
        }

        // Bazada bor nomlar (kichik harfda) — har bir qator uchun so'rov yubormaslik uchun
        Set<String> mavjud = new LinkedHashSet<>();
        for (Kategoriya k : kategoriyaRepository.findAll()) {
            if (k.getNomi() != null) mavjud.add(kalit(k.getNomi()));
        }

        Set<String> fayldaKorilgan = new LinkedHashSet<>();
        List<String> qoshilganlar = new ArrayList<>();
        List<String> otkazilgan = new ArrayList<>();
        List<Kategoriya> yangilar = new ArrayList<>();

        for (String nomi : nomlar) {
            String kalit = kalit(nomi);

            if (!fayldaKorilgan.add(kalit)) {
                otkazilgan.add(nomi + " — faylda takrorlangan");
                continue;
            }
            if (mavjud.contains(kalit)) {
                otkazilgan.add(nomi + " — bazada allaqachon bor");
                continue;
            }

            Kategoriya k = new Kategoriya();
            k.setNomi(nomi);
            yangilar.add(k);
            qoshilganlar.add(nomi);
        }

        if (!yangilar.isEmpty()) {
            kategoriyaRepository.saveAll(yangilar);
        }

        int faylTakror = (int) otkazilgan.stream().filter(t -> t.endsWith("faylda takrorlangan")).count();
        int bazaTakror = otkazilgan.size() - faylTakror;

        tarixService.yoz("Kategoriya", "Exceldan yuklandi", null,
                file.getOriginalFilename(),
                "O'qildi: " + nomlar.size() + " | Qo'shildi: " + qoshilganlar.size() +
                        " | O'tkazib yuborildi: " + otkazilgan.size() +
                        (qoshilganlar.isEmpty() ? "" : " | " + String.join(", ", qoshilganlar)));

        String xulosa = qoshilganlar.isEmpty()
                ? "Yangi kategoriya topilmadi — barchasi allaqachon bazada bor"
                : qoshilganlar.size() + " ta kategoriya qo'shildi";
        if (!otkazilgan.isEmpty()) {
            xulosa += ", " + otkazilgan.size() + " tasi o'tkazib yuborildi";
        }

        return new ImportNatijaDto(true, xulosa, nomlar.size(), qoshilganlar.size(),
                bazaTakror, faylTakror, qoshilganlar, otkazilgan);
    }

    // ================= YORDAMCHI =================

    /** "UZS" yoki "USD" — noma'lum/bo'sh qiymat "UZS"ga tushadi (narxi bo'lmasa ahamiyatsiz) */
    private String valyutaAniqla(KategoriyaSaveDto dto) {
        return "USD".equals(dto.valyuta()) ? "USD" : "UZS";
    }

    /** Taqqoslash uchun kalit — registr va ortiqcha bo'shliqlarga bog'liq emas */
    private String kalit(String nomi) {
        return nomi.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String nomiTekshir(KategoriyaSaveDto dto, Long ozId) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Kategoriya nomi kiritilishi shart";
        }
        Optional<Kategoriya> mavjud = kategoriyaRepository.findByNomiIgnoreCase(dto.nomi().trim());
        if (mavjud.isPresent() && !Objects.equals(mavjud.get().getId(), ozId)) {
            return "Bunday nomli kategoriya allaqachon mavjud";
        }
        if (dto.narxi() != null && dto.narxi() < 0) {
            return "Narxi manfiy bo'lishi mumkin emas";
        }
        return null;
    }
}