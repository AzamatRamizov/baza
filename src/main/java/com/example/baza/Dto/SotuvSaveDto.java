package com.example.baza.Dto;

/**
 * Mahsulotni sotish (yoki KATMga o'tkazish) so'rovi.
 *
 * @param metr         METRAJ mahsulot uchun: kesib beriladigan BO'YI (metr).
 *                     Berilsa miqdor undan hisoblanadi: kv.metr -> metr × eni, metr -> metr.
 * @param miqdor       Birlikdagi miqdor (dona uchun; metr berilmasa ishlatiladi)
 * @param birlikNarxi  1 birlik uchun sotuv narxi (metrajda 1 KV.METR uchun).
 *                     umumiySumma berilsa e'tiborga olinmaydi.
 * @param umumiySumma  Kelishilgan UMUMIY summa — berilsa birlikNarxi o'rniga shu ishlatiladi,
 *                     1 birlik narxi shundan orqaga hisoblanadi (faqat yozuv uchun)
 * @param valyuta      "USD" yoki "UZS" — USD bo'lsa CBU kursi bo'yicha so'mga aylantiriladi
 *                     (birlikNarxi va umumiySumma ikkalasiga ham tegishli)
 * @param muddat        Nasiya muddati (oy) — KATM uchun
 * @param oldindanTulov Boshlang'ich naqd to'lov — KATM uchun (gilamga uzatiladi)
 */
public record SotuvSaveDto(Long mahsulotId,
                           Double metr,
                           Double miqdor,
                           Double birlikNarxi,
                           Double umumiySumma,
                           String valyuta,
                           String mijozIsmi,
                           String mijozTel,
                           Integer muddat,
                           Long oldindanTulov,
                           String izoh) {
}
