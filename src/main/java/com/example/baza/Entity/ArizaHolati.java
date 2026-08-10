package com.example.baza.Entity;

/** Instagramdan kelgan arizaning ish jarayonidagi holati */
public enum ArizaHolati {

    YANGI("Yangi"),
    BOGLANILDI("Bog'lanildi"),
    MIJOZGA_AYLANDI("Mijozga aylandi"),
    RAD_ETILDI("Rad etildi");

    private final String nomi;

    ArizaHolati(String nomi) {
        this.nomi = nomi;
    }

    public String getNomi() {
        return nomi;
    }
}
