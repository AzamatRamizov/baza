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

    public MahsulotService(MahsulotRepository mahsulotRepository,
                           MagazinRepository magazinRepository,
                           UsersRepository usersRepository,
                           KategoriyaRepository kategoriyaRepository,
                           ValyutaService valyutaService) {
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.kategoriyaRepository = kategoriyaRepository;
        this.valyutaService = valyutaService;
    }

    public List<MahsulotDto> getAllMahsulotlar() {
        return mahsulotRepository.findAllDto();
    }

    @Transactional
    public ApiResponse addMahsulot(MahsulotSaveDto dto) {
        String xato = tekshir(dto, null);
        if (xato != null) return new ApiResponse(xato, false);

        Long narxSom = narxniSomgaAylantir(dto);
        if (narxSom == null && dto.zavodNarxi() != null) {
            return new ApiResponse(
                    "Dollar kursini olib bo'lmadi — keyinroq urinib ko'ring yoki narxni so'mda kiriting", false);
        }

        Mahsulot mahsulot = new Mahsulot();
        maydonlarniTuldirish(mahsulot, dto, narxSom);

        // Yaratgan user — SecurityContext'dan (faqat yaratishda)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        usersRepository.findByUsername(username).ifPresent(mahsulot::setYaratganUser);

        mahsulotRepository.save(mahsulot);
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

        Long narxSom = narxniSomgaAylantir(dto);
        if (narxSom == null && dto.zavodNarxi() != null) {
            return new ApiResponse(
                    "Dollar kursini olib bo'lmadi — keyinroq urinib ko'ring yoki narxni so'mda kiriting", false);
        }

        maydonlarniTuldirish(mahsulot, dto, narxSom); // yaratganUser o'zgarmaydi
        mahsulotRepository.save(mahsulot);
        return new ApiResponse("Mahsulot yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteMahsulot(Long id) {
        if (!mahsulotRepository.existsById(id)) {
            return new ApiResponse("Mahsulot topilmadi", false);
        }
        mahsulotRepository.deleteById(id);
        return new ApiResponse("Mahsulot o'chirildi", true);
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
        if (dto.zavodNarxi() != null && dto.zavodNarxi() < 0) {
            return "Zavod narxi manfiy bo'lishi mumkin emas";
        }
        if ((dto.boyi() != null && dto.boyi() < 0) || (dto.eni() != null && dto.eni() < 0)) {
            return "O'lchamlar manfiy bo'lishi mumkin emas";
        }
        return null;
    }

    /**
     * Zavod narxi 1 KV METR uchun kiritiladi. Umumiy narx:
     *   umumiy = kv (boyi * eni) * 1 kv narxi
     * Masalan 3x4 (12 kv), 3.3$ dan: 12 * 3.3 = 39.6$ -> kurs bo'yicha so'mga.
     * - valyuta "USD" bo'lsa — CBU kursi bo'yicha so'mga (kurs topilmasa null),
     * - so'm bo'lsa — so'mligicha.
     * O'lchamlar kiritilmagan bo'lsa (kv yo'q) — kiritilgan qiymat umumiy narx deb olinadi.
     */
    private Long narxniSomgaAylantir(MahsulotSaveDto dto) {
        if (dto.zavodNarxi() == null) return null;

        double narx = dto.zavodNarxi();

        if ("USD".equals(dto.valyuta())) {
            UsdKursDto kurs = valyutaService.getUsdKurs();
            if (kurs.kurs() == null) return null; // kurs olinmadi
            narx *= kurs.kurs();
        }

        Double kvVal = kvHisobla(dto.boyi(), dto.eni());
        if (kvVal != null && kvVal > 0) {
            narx *= kvVal;
        }

        return Math.round(narx);
    }

    private void maydonlarniTuldirish(Mahsulot mahsulot, MahsulotSaveDto dto, Long narxSom) {
        mahsulot.setNomi(dto.nomi().trim());
        mahsulot.setKod(dto.kod().trim());
        mahsulot.setKategoriya(dto.kategoriyaId() == null
                ? null
                : kategoriyaRepository.findById(dto.kategoriyaId()).orElse(null));
        mahsulot.setBirlik(dto.birlik());
        mahsulot.setZavodNarxi(narxSom);
        mahsulot.setBoyi(dto.boyi());
        mahsulot.setEni(dto.eni());
        mahsulot.setKv(kvHisobla(dto.boyi(), dto.eni()));
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
