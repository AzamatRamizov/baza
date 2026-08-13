package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Kategoriya extends AbstractLongEntity {

    private String nomi;

    /**
     * Standart tan narxi (1 kv.metr uchun) — mahsulot qo'shishda shu kategoriya
     * tanlansa, "Zavod narxi" maydoniga avtomatik taklif sifatida qo'yiladi.
     * Kiritilgan valyutaning o'zida saqlanadi (so'mga aylantirilmaydi) — mahsulot
     * qo'shish formasidagi kabi, USD bo'lsa so'mga aylantirish saqlash vaqtida
     * JORIY kursga qarab amalga oshadi (MahsulotService.narxniSomgaAylantir).
     */
    private Double narxi;

    /** "UZS" yoki "USD" — narxi qaysi valyutada kiritilgan */
    private String valyuta;
}
