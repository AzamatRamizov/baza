package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.InstagramAkkauntDto;
import com.example.baza.Dto.InstagramAkkauntSaveDto;
import com.example.baza.Entity.InstagramAkkaunt;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.InstagramAkkauntRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class InstagramAkkauntService {

    private final InstagramAkkauntRepository instagramAkkauntRepository;
    private final UsersRepository usersRepository;
    private final TarixService tarixService;

    public InstagramAkkauntService(InstagramAkkauntRepository instagramAkkauntRepository,
                                   UsersRepository usersRepository,
                                   TarixService tarixService) {
        this.instagramAkkauntRepository = instagramAkkauntRepository;
        this.usersRepository = usersRepository;
        this.tarixService = tarixService;
    }

    @Transactional(readOnly = true)
    public List<InstagramAkkauntDto> getAllAkkauntlar() {
        return instagramAkkauntRepository.findAllByOrderByNomiAsc().stream()
                .map(this::dto)
                .toList();
    }

    /** Faqat shu xodim mas'ul bo'lgan akkauntlar — ariza qo'shishda akkaunt tanlash uchun */
    @Transactional(readOnly = true)
    public List<InstagramAkkauntDto> getMeningAkkauntlarim(String username) {
        return instagramAkkauntRepository.findAllByMasulUser_UsernameOrderByNomiAsc(username).stream()
                .map(this::dto)
                .toList();
    }

    @Transactional
    public ApiResponse addAkkaunt(InstagramAkkauntSaveDto dto) {
        String xato = tekshir(dto, null);
        if (xato != null) return new ApiResponse(xato, false);

        InstagramAkkaunt akkaunt = new InstagramAkkaunt();
        akkaunt.setNomi(dto.nomi().trim());
        akkaunt.setPageId(dto.pageId().trim());
        akkaunt.setPageAccessToken(dto.pageAccessToken() == null ? null : dto.pageAccessToken().trim());
        akkaunt.setFaol(dto.faol());
        if (dto.masulUserId() != null) {
            akkaunt.setMasulUser(usersRepository.findById(dto.masulUserId()).orElse(null));
        }
        instagramAkkauntRepository.save(akkaunt);

        tarixService.yoz("Instagram akkaunt", "Qo'shildi", akkaunt.getId(), akkaunt.getNomi(), null);
        return new ApiResponse("Instagram akkaunt qo'shildi", true);
    }

    @Transactional
    public ApiResponse updateAkkaunt(Long id, InstagramAkkauntSaveDto dto) {
        InstagramAkkaunt akkaunt = instagramAkkauntRepository.findById(id).orElse(null);
        if (akkaunt == null) {
            return new ApiResponse("Akkaunt topilmadi", false);
        }

        String xato = tekshir(dto, id);
        if (xato != null) return new ApiResponse(xato, false);

        akkaunt.setNomi(dto.nomi().trim());
        akkaunt.setPageId(dto.pageId().trim());
        if (dto.pageAccessToken() != null && !dto.pageAccessToken().isBlank()) {
            akkaunt.setPageAccessToken(dto.pageAccessToken().trim());
        }
        akkaunt.setFaol(dto.faol());
        akkaunt.setMasulUser(dto.masulUserId() == null
                ? null : usersRepository.findById(dto.masulUserId()).orElse(null));
        instagramAkkauntRepository.save(akkaunt);

        tarixService.yoz("Instagram akkaunt", "Tahrirlandi", akkaunt.getId(), akkaunt.getNomi(), null);
        return new ApiResponse("Akkaunt yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteAkkaunt(Long id) {
        InstagramAkkaunt akkaunt = instagramAkkauntRepository.findById(id).orElse(null);
        if (akkaunt == null) {
            return new ApiResponse("Akkaunt topilmadi", false);
        }
        instagramAkkauntRepository.deleteById(id);

        tarixService.yoz("Instagram akkaunt", "O'chirildi", id, akkaunt.getNomi(), null);
        return new ApiResponse("Akkaunt o'chirildi", true);
    }

    private String tekshir(InstagramAkkauntSaveDto dto, Long ozId) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Akkaunt nomi kiritilishi shart";
        }
        if (dto.pageId() == null || dto.pageId().isBlank()) {
            return "Facebook Page ID kiritilishi shart";
        }
        Optional<InstagramAkkaunt> mavjud = instagramAkkauntRepository.findByPageId(dto.pageId().trim());
        if (mavjud.isPresent() && !Objects.equals(mavjud.get().getId(), ozId)) {
            return "Bunday Page ID bilan akkaunt allaqachon mavjud";
        }
        return null;
    }

    private InstagramAkkauntDto dto(InstagramAkkaunt a) {
        Users masul = a.getMasulUser();
        return new InstagramAkkauntDto(a.getId(), a.getNomi(), a.getPageId(),
                a.getPageAccessToken() != null && !a.getPageAccessToken().isBlank(),
                masul == null ? null : masul.getId(),
                masul == null ? null : masul.getFish(),
                a.isFaol());
    }
}
