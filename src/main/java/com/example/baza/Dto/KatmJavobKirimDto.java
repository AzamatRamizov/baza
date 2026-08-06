package com.example.baza.Dto;

/**
 * Gilam dasturidan keladigan javob.
 *
 * @param sotuvId     bazadagi Sotuv id (gilamga yuborilgan)
 * @param tasdiq      true -> shartnoma tuzildi (sotuv yakunlanadi)
 *                    false -> rad etildi (mahsulot qoldig'i tiklanadi)
 * @param izoh        sabab / izoh
 * @param shartnomaId gilamdagi shartnoma raqami (tasdiqlanganda)
 * @param kim         javobni bergan hodim (gilamdagi)
 */
public record KatmJavobKirimDto(Long sotuvId,
                                Boolean tasdiq,
                                String izoh,
                                Long shartnomaId,
                                String kim) {
}
