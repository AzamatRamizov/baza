package com.example.baza.Dto;

/**
 * Mahsulot qo'shish / tahrirlash.
 *
 * BIRLIK KIRITILMAYDI — u TURI dan kelib chiqib aniqlanadi:
 *   gatoviy -> dona   (nechtaligi miqdorda kiritiladi)
 *   metraj  -> kv.metr (eni bo'lsa) yoki metr (eni bo'lmasa)
 */
public record MahsulotSaveDto(String nomi,
                              String kod,
                              Long kategoriyaId,
                              Double zavodNarxi,
                              String valyuta,
                              Double boyi,
                              Double eni,
                              Double miqdor,
                              String turi,
                              Long magazinId) {
}