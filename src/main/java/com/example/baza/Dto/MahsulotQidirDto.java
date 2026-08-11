package com.example.baza.Dto;

/**
 * Shtrix-kod/QR skaneri orqali kod bo'yicha mahsulot qidirish natijasi.
 *
 * @param tegishli qidiruvchi hodimga (uning magazin(lar)iga) tegishlimi — Owner uchun
 *                 har doim true; ko'plik topilganda hisoblanmaydi (har doim true qaytadi,
 *                 chaqiruvchi ro'yxatga o'tkazadi, tegishlilikni u yerda ko'radi)
 */
public record MahsulotQidirDto(Long id, boolean koplik, String kod, boolean tegishli,
                               String nomi, String magazinNomi) {
}
