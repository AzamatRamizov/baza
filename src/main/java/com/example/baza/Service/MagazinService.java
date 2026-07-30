package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.MagazinDto;
import com.example.baza.Dto.MagazinQisqaDto;
import com.example.baza.Dto.MagazinSaveDto;
import com.example.baza.Dto.RolQisqaDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MagazinService {

    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;

    public MagazinService(MagazinRepository magazinRepository,
                          UsersRepository usersRepository) {
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
    }

    public List<MagazinDto> getAllMagazinlar() {
        return magazinRepository.findAllWithHodimlar().stream()
                .map(m -> new MagazinDto(
                        m.getId(),
                        m.getNomi(),
                        m.getHodimlar().stream()
                                .map(u -> new UserDto(u.getId(), u.getFish(), u.getTel(),
                                        u.getAddress(), u.getIzoh(), u.getUsername(),
                                        u.getRollar().stream()
                                                .map(r -> new RolQisqaDto(r.getId(), r.getNomi()))
                                                .toList(),
                                        null, null))
                                .toList()))
                .toList();
    }

    /** Select ro'yxatlari uchun qisqa ro'yxat — barcha login qilgan userlarga ochiq */
    public List<MagazinQisqaDto> getMagazinNomlar() {
        return magazinRepository.findAll().stream()
                .map(m -> new MagazinQisqaDto(m.getId(), m.getNomi()))
                .toList();
    }

    @Transactional
    public ApiResponse addMagazin(MagazinSaveDto dto) {
        String xato = nomiTekshir(dto, null);
        if (xato != null) return new ApiResponse(xato, false);

        Magazin magazin = new Magazin();
        magazin.setNomi(dto.nomi().trim());
        magazin.setHodimlar(hodimlarniYukla(dto.hodimIds()));
        magazinRepository.save(magazin);

        return new ApiResponse("Magazin qo'shildi", true);
    }

    /**
     * Tahrirlash: nomi yangilanadi, hodimlar ro'yxati kelgan id'lar bilan
     * TO'LIQ almashtiriladi — shu orqali qo'shish, olib tashlash va
     * almashtirish bitta amalda bajariladi.
     */
    @Transactional
    public ApiResponse updateMagazin(Long id, MagazinSaveDto dto) {
        Magazin magazin = magazinRepository.findById(id).orElse(null);
        if (magazin == null) {
            return new ApiResponse("Magazin topilmadi", false);
        }

        String xato = nomiTekshir(dto, id);
        if (xato != null) return new ApiResponse(xato, false);

        magazin.setNomi(dto.nomi().trim());
        magazin.setHodimlar(hodimlarniYukla(dto.hodimIds()));
        magazinRepository.save(magazin);

        return new ApiResponse("Magazin yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteMagazin(Long id) {
        Magazin magazin = magazinRepository.findById(id).orElse(null);
        if (magazin == null) {
            return new ApiResponse("Magazin topilmadi", false);
        }
        magazin.getHodimlar().clear(); // avval bog'lanish jadvalini tozalaymiz
        magazinRepository.delete(magazin);
        return new ApiResponse("Magazin o'chirildi", true);
    }

    // ================= YORDAMCHI =================

    private String nomiTekshir(MagazinSaveDto dto, Long ozId) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Magazin nomi kiritilishi shart";
        }
        Optional<Magazin> mavjud = magazinRepository.findByNomiIgnoreCase(dto.nomi().trim());
        if (mavjud.isPresent() && !Objects.equals(mavjud.get().getId(), ozId)) {
            return "Bunday nomli magazin allaqachon mavjud";
        }
        return null;
    }

    private LinkedHashSet<Users> hodimlarniYukla(List<Long> hodimIds) {
        if (hodimIds == null || hodimIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(usersRepository.findAllById(hodimIds));
    }
}
