package com.example.baza.Entity;

/**
 * Sotuv turi — mahsulot qanday chiqib ketdi:
 *   NAQD — oddiy sotuv (pul olindi, tugadi)
 *   KATM — nasiyaga: mijoz KATM (kredit tarixi) tekshiruvidan o'tishi kerak,
 *          shartnoma boshqa dasturda rasmiylashtiriladi
 */
public enum SotuvTuri {

    NAQD("Naqd sotuv"),
    KATM("KATM (nasiya)");

    private final String nomi;

    SotuvTuri(String nomi) {
        this.nomi = nomi;
    }

    public String getNomi() {
        return nomi;
    }
}
