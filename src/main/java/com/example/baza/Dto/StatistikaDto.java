package com.example.baza.Dto;

import java.util.List;

/**
 * Statistika sahifasi uchun umumiy ko'rsatkichlar.
 *
 * Owner uchun — butun tizim bo'yicha (barcha magazinlar), hodim uchun — faqat
 * o'zi mas'ul bo'lgan magazin(lar) bo'yicha (mahsulot/sotuv ro'yxatlari bilan
 * bir xil qoidada, {@code owner} maydoni frontendga qaysi rejimda ekanini bildiradi).
 *
 * @param hodimSoni null bo'lsa — frontend bu ko'rsatkichni yashiradi (hodimga
 *                  butun tashkilot bo'yicha xodimlar sonini ko'rsatmaymiz)
 */
public record StatistikaDto(boolean owner,
                            long mahsulotSoni,
                            double umumiyKv,
                            long umumiyZavodQiymati,
                            long magazinSoni,
                            Long hodimSoni,
                            long kategoriyaSoni,
                            long sotuvSoni,
                            long sotuvSummasi,
                            long sotuvFoyda,
                            long bugungiSotuvSoni,
                            long bugungiSotuvSummasi,
                            long katmSoni,
                            long katmSummasi,
                            long otkazmaKutilmoqda,
                            List<MagazinStatDto> magazinlar) {
}
