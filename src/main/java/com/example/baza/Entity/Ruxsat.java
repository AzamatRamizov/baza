package com.example.baza.Entity;

/**
 * Tizimda mavjud barcha ruxsatlar (amallar).
 *
 * YANGI AMAL QO'SHISH TARTIBI:
 *  1. Shu enum'ga yangi konstanta qo'shing (nomi + bo'limi bilan)
 *  2. Controller'dagi endpoint'ga @PreAuthorize("hasAuthority('YANGI_KOD')") qo'ying
 *  Ruxsatlar sahifasi enum'dan avtomatik o'qiydi — sahifani o'zgartirish SHART EMAS.
 */
public enum Ruxsat {

    // ===== Hodimlar =====
    HODIM_BOSHQARISH("Hodimlarni boshqarish (qo'shish, o'chirish, rol biriktirish)", "Hodimlar"),
    ROL_BOSHQARISH("Rollar va ruxsatlarni boshqarish", "Hodimlar"),

    // ===== Magazinlar =====
    MAGAZIN_BOSHQARISH("Magazinlarni boshqarish (qo'shish, tahrirlash, o'chirish)", "Magazinlar"),

    // ===== Mahsulotlar =====
    MAHSULOT_QOSHISH("Mahsulot qo'shish va tahrirlash", "Mahsulotlar"),
    MAHSULOT_OCHIRISH("Mahsulot o'chirish", "Mahsulotlar"),
    KATEGORIYA_BOSHQARISH("Kategoriyalarni boshqarish", "Mahsulotlar"),

    // ===== O'tkazmalar =====
    OTKAZMA_YUBORISH("Mahsulotni boshqa magazinga jo'natish", "O'tkazmalar"),
    OTKAZMA_QABUL("Kelgan o'tkazmani tasdiqlash yoki rad etish", "O'tkazmalar"),

    // ===== Sotuv =====
    SOTUV_QILISH("Mahsulot sotish", "Sotuv"),
    SOTUV_KORISH("Sotuvlarni ko'rish", "Sotuv"),
    SOTUV_QAYTARISH("Sotuvni qaytarish (bekor qilish)", "Sotuv"),

    // ===== Tarix =====
    TARIX_KORISH("Amallar tarixini ko'rish", "Tarix");

    private final String nomi;
    private final String bolim;

    Ruxsat(String nomi, String bolim) {
        this.nomi = nomi;
        this.bolim = bolim;
    }

    public String getNomi() {
        return nomi;
    }

    public String getBolim() {
        return bolim;
    }
}