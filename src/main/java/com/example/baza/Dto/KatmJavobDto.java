package com.example.baza.Dto;

/**
 * KATM javobi.
 *
 * @param tasdiq true  -> KATM ruxsat berdi, sotuv yakunlanadi (SOTILDI)
 *               false -> rad etildi, mahsulot qoldig'i tiklanadi (KATM_RAD)
 * @param izoh   sabab / izoh (ixtiyoriy)
 */
public record KatmJavobDto(Boolean tasdiq, String izoh) {
}
