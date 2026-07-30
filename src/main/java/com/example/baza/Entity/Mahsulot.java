package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Mahsulot extends AbstractLongEntity {

    private String nomi;

    /** Mahsulot kodi (artikul) — takrorlanmas */
    private String kod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kategoriya_id")
    private Kategoriya kategoriya;

    /** O'lchov birligi: dona, metr, kv.metr ... */
    private String birlik;

    /** Zavod (tannarx) narxi, so'mda */
    private Long zavodNarxi;

    /** O'lchamlar (metrda) */
    private Double boyi;
    private Double eni;

    /** Kvadrat (avtomatik: boyi * eni) — servisda hisoblanadi */
    private Double kv;

    /** Turi: "gatoviy" yoki "metraj" */
    private String turi;

    /** Qaysi magazinga tegishli */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazin_id")
    private Magazin magazin;

    /** Kim yaratgan (avtomatik, SecurityContext'dan) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yaratgan_user_id", updatable = false)
    private Users yaratganUser;
}
