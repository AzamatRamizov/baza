package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Repository.KategoriyaRepository;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MahsulotService {

    private final MahsulotRepository mahsulotRepository;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final KategoriyaRepository kategoriyaRepository;

    public MahsulotService(MahsulotRepository mahsulotRepository,
                           MagazinRepository magazinRepository,
                           UsersRepository usersRepository,
                           KategoriyaRepository kategoriyaRepository) {
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.kategoriyaRepository = kategoriyaRepository;
    }

    public List<MahsulotDto> getAllMahsulotlar() {
        return mahsulotRepository.findAllDto();
    }

    @Transactional
    public ApiResponse addMahsulot(MahsulotSaveDto dto) {
        String xato = tekshir(dto);
        if (xato != null) return new ApiResponse(xato, false);

        Mahsulot mahsulot = new Mahsulot();
        maydonlarniTuldirish(mahsulot, dto);

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

        String xato = tekshir(dto);
        if (xato != null) return new ApiResponse(xato, false);

        maydonlarniTuldirish(mahsulot, dto); // yaratganUser o'zgarmaydi
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

    private String tekshir(MahsulotSaveDto dto) {
        if (dto.nomi() == null || dto.nomi().isBlank()) {
            return "Mahsulot nomi kiritilishi shart";
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

    private void maydonlarniTuldirish(Mahsulot mahsulot, MahsulotSaveDto dto) {
        mahsulot.setNomi(dto.nomi().trim());
        mahsulot.setKategoriya(dto.kategoriyaId() == null
                ? null
                : kategoriyaRepository.findById(dto.kategoriyaId()).orElse(null));
        mahsulot.setBirlik(dto.birlik());
        mahsulot.setZavodNarxi(dto.zavodNarxi());
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
