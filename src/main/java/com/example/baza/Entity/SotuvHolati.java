package com.example.baza.Entity;

/** Sotuv holati */
public enum SotuvHolati {

    /** Naqd sotildi yoki KATM tasdiqladi — yakuniy holat */
    SOTILDI("Sotildi"),

    /** KATMga o'tkazildi, javob kutilmoqda (mahsulot band, qoldiqdan chiqib ketgan) */
    KATMDA("KATMda"),

    /** KATM rad etdi — mahsulot qoldig'i tiklanadi */
    KATM_RAD("KATM rad etdi"),

    /** Sotuv bekor qilindi — mahsulot qoldig'i tiklanadi */
    QAYTARILDI("Qaytarildi");

    private final String nomi;

    SotuvHolati(String nomi) {
        this.nomi = nomi;
    }

    public String getNomi() {
        return nomi;
    }
}
