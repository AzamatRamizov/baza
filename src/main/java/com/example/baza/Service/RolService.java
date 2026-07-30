package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.RolDto;
import com.example.baza.Dto.RolSaveDto;
import com.example.baza.Dto.RuxsatDto;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Ruxsat;
import com.example.baza.Repository.RolRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final UsersRepository usersRepository;

    public RolService(RolRepository rolRepository, UsersRepository usersRepository) {
        this.rolRepository = rolRepository;
        this.usersRepository = usersRepository;
    }

    /** Tizimdagi barcha ruxsat turlari — enum'dan avtomatik */
    public List<RuxsatDto> getRuxsatTurlari() {
        return Arrays.stream(Ruxsat.values())
                .map(r -> new RuxsatDto(r.name(), r.getNomi(), r.getBolim()))
                .toList();
    }

    public List<RolDto> getAllRollar() {
        return rolRepository.findAllByOrderByNomiAsc().stream()
                .map(r -> new RolDto(
                        r.getId(),
                        r.getNomi(),
                        r.getRuxsatlar().stream().map(Enum::name).toList(),
                        usersRepository.countByRollar_Id(r.getId()),
                        r.isTizimRoli(),
                        r.isMenejerKerak()))
                .toList();
    }

    @Transactional
    public ApiResponse addRol(RolSaveDto dto) {
        String xato = nomiTekshir(dto, null);
        if (xato != null) return new ApiResponse(xato, false);

        Rol rol = new Rol();
        rol.setNomi(dto.nomi().trim());
        rol.setMenejerKerak(Boolean.TRUE.equals(dto.menejerKerak()));
        rolRepository.save(rol);

        return new ApiResponse("Rol qo'shildi", true);
    }

    @Transactional
    public ApiResponse updateRol(Long id, RolSaveDto dto) {
        Rol rol = rolRepository.findById(id).orElse(null);
        if (rol == null) {
            return new ApiResponse("Rol topilmadi", false);
        }

        if (rol.isTizimRoli()) {
            return new ApiResponse("Owner tizim roli — nomi o'zgartirilmaydi", false);
        }

        String xato = nomiTekshir(dto, id);
        if (xato != null) return new ApiResponse(xato, false);

        rol.setNomi(dto.nomi().trim());
        rol.setMenejerKerak(Boolean.TRUE.equals(dto.menejerKerak()));
        rolRepository.save(rol);

        return new ApiResponse("Rol yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteRol(Long id) {
        Rol rol = rolRepository.findById(id).orElse(null);
        if (rol == null) {
            return new ApiResponse("Rol topilmadi", false);
        }
        if (rol.isTizimRoli()) {
            return new ApiResponse("Owner tizim roli — o'chirilmaydi", false);
        }

        long userSoni = usersRepository.countByRollar_Id(id);
        if (userSoni > 0) {
            return new ApiResponse(
                    "Bu rolda " + userSoni + " ta hodim bor — avval ularga boshqa rol biriktiring", false);
        }

        rolRepository.deleteById(id);
        return new ApiResponse("Rol o'chirildi", true);
    }

    /** Rolning ruxsatlarini to'liq almashtirish (ruxsatlar sahifasidan) */
    @Transactional
    public ApiResponse updateRuxsatlar(Long rolId, List<String> kodlar) {
        Rol rol = rolRepository.findById(rolId).orElse(null);
        if (rol == null) {
            return new ApiResponse("Rol topilmadi", false);
        }
        if (rol.isTizimRoli()) {
            return new ApiResponse("Owner tizim roli doim barcha ruxsatlarga ega — o'zgartirilmaydi", false);
        }

        LinkedHashSet<Ruxsat> yangi = new LinkedHashSet<>();
        if (kodlar != null) {
            for (String kod : kodlar) {
                try {
                    yangi.add(Ruxsat.valueOf(kod));
                } catch (IllegalArgumentException e) {
                    return new ApiResponse("Noma'lum ruxsat kodi: " + kod, false);
                }
            }
        }

        rol.setRuxsatlar(yangi);
        rolRepository.save(rol);

        return new ApiResponse("\"" + rol.getNomi() + "\" ruxsatlari saqlandi", true);
    }

    // ================= YORDAMCHI =================

    private String nomiTekshir(RolSaveDto dto, Long ozId) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Rol nomi kiritilishi shart";
        }
        Optional<Rol> mavjud = rolRepository.findByNomiIgnoreCase(dto.nomi().trim());
        if (mavjud.isPresent() && !Objects.equals(mavjud.get().getId(), ozId)) {
            return "Bunday nomli rol allaqachon mavjud";
        }
        return null;
    }
}
