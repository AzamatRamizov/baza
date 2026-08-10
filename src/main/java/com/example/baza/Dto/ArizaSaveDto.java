package com.example.baza.Dto;

/** Qo'lda ariza qo'shish uchun. instagramAkkauntId ixtiyoriy — belgilansa, o'sha akkauntga mas'ul xodim buni ham ko'radi */
public record ArizaSaveDto(String ismFamiliya, String telefon, String izoh, Long instagramAkkauntId) {
}
