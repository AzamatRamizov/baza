package com.example.baza.Dto;

import java.util.List;

/**
 * Exceldan import natijasi.
 *
 * @param holat        umuman muvaffaqiyatlimi (fayl o'qildimi)
 * @param message      qisqacha xulosa (notification uchun)
 * @param oqildi       fayldan o'qilgan bo'sh bo'lmagan nomlar soni
 * @param qoshildi     yangi qo'shilganlar soni
 * @param takror       bazada allaqachon bor bo'lgani uchun o'tkazib yuborilganlar
 * @param faylTakror   faylning o'zida takrorlangani uchun o'tkazib yuborilganlar
 * @param qoshilganlar qo'shilgan nomlar (ko'rsatish uchun)
 * @param otkazilgan   o'tkazib yuborilgan nomlar (sababi bilan)
 */
public record ImportNatijaDto(boolean holat,
                              String message,
                              int oqildi,
                              int qoshildi,
                              int takror,
                              int faylTakror,
                              List<String> qoshilganlar,
                              List<String> otkazilgan) {

    public static ImportNatijaDto xato(String message) {
        return new ImportNatijaDto(false, message, 0, 0, 0, 0, List.of(), List.of());
    }
}