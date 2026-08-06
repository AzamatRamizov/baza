package com.example.baza.Dto;

import java.util.List;

/**
 * Mahsulotlarni Exceldan ommaviy import qilish natijasi.
 *
 * @param holat      umuman muvaffaqiyatlimi (fayl o'qildimi)
 * @param message    qisqacha xulosa (notification uchun)
 * @param oqildi     fayldan o'qilgan (bo'sh bo'lmagan "Товар" ustunli) qatorlar soni
 * @param qoshildi   yangi qo'shilgan mahsulotlar soni
 * @param otkazibYuborildi o'tkazib yuborilgan (xato/ziddiyatli) qatorlar soni
 * @param xatolar    o'tkazib yuborilgan qatorlar sababi bilan (ko'rsatish uchun, cheklangan sonda)
 * @param qisqartirilgan xatolar ro'yxati cheklangani uchun qisqartirilganmi
 */
public record MahsulotImportNatijaDto(boolean holat,
                                      String message,
                                      int oqildi,
                                      int qoshildi,
                                      int otkazibYuborildi,
                                      List<String> xatolar,
                                      boolean qisqartirilgan) {

    public static MahsulotImportNatijaDto xato(String message) {
        return new MahsulotImportNatijaDto(false, message, 0, 0, 0, List.of(), false);
    }
}
