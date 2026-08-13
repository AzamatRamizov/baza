package com.example.baza.Dto;

/**
 * @param tegishli ro'yxat so'rovlarida (findAllDto/findMagazinimDto) hisoblanmaydi, doim
 *                 {@code true} keladi — faqat bitta mahsulot olinganda (findDtoById)
 *                 {@link com.example.baza.Service.MahsulotService#getMahsulotDto} joriy
 *                 hodimga nisbatan haqiqiy qiymat bilan almashtiradi.
 * @param sotilganSumma xuddi {@code tegishli} kabi — ro'yxat so'rovlarida hisoblanmaydi
 *                 (doim {@code 0}), faqat bitta mahsulot olinganda shu mahsulot bo'yicha
 *                 jami sotilgan summa bilan to'ldiriladi.
 */
public record MahsulotDto(Long id,
                          String nomi,
                          String kod,
                          String serialKod,
                          Long kategoriyaId,
                          String kategoriyaNomi,
                          String birlik,
                          Long zavodNarxi,
                          Double boyi,
                          Double eni,
                          Double kv,
                          Double miqdor,
                          String turi,
                          Long magazinId,
                          String magazinNomi,
                          String yaratganFish,
                          String rasm,
                          boolean tegishli,
                          long sotilganSumma) {

    /** Detail sahifasi uchun — joriy hodimga nisbatan hisoblangan maydonlarni to'ldiradi */
    public MahsulotDto detailUchunTuldirish(boolean tegishli, long sotilganSumma) {
        return new MahsulotDto(id, nomi, kod, serialKod, kategoriyaId, kategoriyaNomi, birlik, zavodNarxi,
                boyi, eni, kv, miqdor, turi, magazinId, magazinNomi, yaratganFish, rasm,
                tegishli, sotilganSumma);
    }
}
