package com.example.baza.Dto;

/**
 * Mahsulotni sotish so'rovi.
 *
 * @param birlikNarxi 1 birlik uchun sotuv narxi
 * @param valyuta     "USD" yoki "UZS" — USD bo'lsa CBU kursi bo'yicha so'mga aylantiriladi
 */
public record SotuvSaveDto(Long mahsulotId,
                           Double miqdor,
                           Double birlikNarxi,
                           String valyuta,
                           String mijozIsmi,
                           String mijozTel,
                           String izoh) {
}
