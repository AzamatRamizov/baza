package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KategoriyaDto;
import com.example.baza.Dto.KategoriyaSaveDto;
import com.example.baza.Entity.Kategoriya;
import com.example.baza.Repository.KategoriyaRepository;
import com.example.baza.Repository.MahsulotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class KategoriyaService {

    private final KategoriyaRepository kategoriyaRepository;
    private final MahsulotRepository mahsulotRepository;

    public KategoriyaService(KategoriyaRepository kategoriyaRepository,
                             MahsulotRepository mahsulotRepository) {
        this.kategoriyaRepository = kategoriyaRepository;
        this.mahsulotRepository = mahsulotRepository;
    }

    public List<KategoriyaDto> getAllKategoriyalar() {
        return kategoriyaRepository.findAllByOrderByNomiAsc().stream()
                .map(k -> new KategoriyaDto(k.getId(), k.getNomi()))
                .toList();
    }

    @Transactional
    public ApiResponse addKategoriya(KategoriyaSaveDto dto) {
        String xato = nomiTekshir(dto, null);
        if (xato != null) return new ApiResponse(xato, false);

        Kategoriya kategoriya = new Kategoriya();
        kategoriya.setNomi(dto.nomi().trim());
        kategoriyaRepository.save(kategoriya);

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

        kategoriya.setNomi(dto.nomi().trim());
        kategoriyaRepository.save(kategoriya);

        return new ApiResponse("Kategoriya yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteKategoriya(Long id) {
        if (!kategoriyaRepository.existsById(id)) {
            return new ApiResponse("Kategoriya topilmadi", false);
        }

        long mahsulotSoni = mahsulotRepository.countByKategoriya_Id(id);
        if (mahsulotSoni > 0) {
            return new ApiResponse(
                    "Bu kategoriyada " + mahsulotSoni + " ta mahsulot bor — " +
                    "avval ularni boshqa kategoriyaga o'tkazing", false);
        }

        kategoriyaRepository.deleteById(id);
        return new ApiResponse("Kategoriya o'chirildi", true);
    }

    // ================= YORDAMCHI =================

    private String nomiTekshir(KategoriyaSaveDto dto, Long ozId) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Kategoriya nomi kiritilishi shart";
        }
        Optional<Kategoriya> mavjud = kategoriyaRepository.findByNomiIgnoreCase(dto.nomi().trim());
        if (mavjud.isPresent() && !Objects.equals(mavjud.get().getId(), ozId)) {
            return "Bunday nomli kategoriya allaqachon mavjud";
        }
        return null;
    }
}
