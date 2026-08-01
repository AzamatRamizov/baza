package com.example.baza.Entity;

/** Mahsulot o'tkazmasining holati */
public enum OtkazmaHolati {

    KUTILMOQDA("Kutilmoqda"),
    QABUL_QILINDI("Qabul qilindi"),
    RAD_ETILDI("Rad etildi"),
    BEKOR_QILINDI("Bekor qilindi");

    private final String nomi;

    OtkazmaHolati(String nomi) {
        this.nomi = nomi;
    }

    public String getNomi() {
        return nomi;
    }
}
