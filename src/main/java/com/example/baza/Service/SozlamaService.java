package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.SozlamaDto;
import com.example.baza.Entity.Sozlama;
import com.example.baza.Repository.SozlamaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tizim sozlamalari — yagona (singleton) qator sifatida saqlanadi */
@Service
public class SozlamaService {

    private final SozlamaRepository sozlamaRepository;

    public SozlamaService(SozlamaRepository sozlamaRepository) {
        this.sozlamaRepository = sozlamaRepository;
    }

    @Transactional
    public SozlamaDto olish() {
        Sozlama s = yagonaQator();
        return new SozlamaDto(s.getChopEtishNusxaSoni());
    }

    @Transactional
    public ApiResponse yangilash(SozlamaDto dto) {
        if (dto.chopEtishNusxaSoni() == null || dto.chopEtishNusxaSoni() < 1 || dto.chopEtishNusxaSoni() > 20) {
            return new ApiResponse("Nusxalar soni 1 dan 20 gacha bo'lishi kerak", false);
        }

        Sozlama s = yagonaQator();
        s.setChopEtishNusxaSoni(dto.chopEtishNusxaSoni());
        sozlamaRepository.save(s);
        return new ApiResponse("Sozlamalar saqlandi", true);
    }

    /** Jadvalda birinchi (va yagona) qatorni qaytaradi, hali bo'lmasa standart qiymatlar bilan yaratadi */
    private Sozlama yagonaQator() {
        List<Sozlama> hammasi = sozlamaRepository.findAll();
        if (!hammasi.isEmpty()) return hammasi.get(0);
        return sozlamaRepository.save(new Sozlama());
    }
}
