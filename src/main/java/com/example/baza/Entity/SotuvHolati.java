package com.example.baza.Entity;

/** Sotuv holati */
public enum SotuvHolati {

    SOTILDI("Sotildi"),
    QAYTARILDI("Qaytarildi");

    private final String nomi;

    SotuvHolati(String nomi) {
        this.nomi = nomi;
    }

    public String getNomi() {
        return nomi;
    }
}
