package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import com.example.baza.Configurations.SotuvHolatiConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mahsulot sotuvi.
 *
 * Sotuvchi FAQAT o'zi mas'ul bo'lgan magazindagi mahsulotni sotadi.
 * Sotilganda mahsulotning qoldig'i (miqdor) kamayadi, tannarxi ham
 * proporsional ravishda ayriladi — shuning uchun foydani hisoblash mumkin.
 * Qaytarilganda hammasi teskarisiga tiklanadi.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Sotuv extends AbstractLongEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mahsulot_id")
    private Mahsulot mahsulot;

    /** Mahsulot keyin o'chirilsa ham sotuv tarixi ko'rinib tursin */
    @Column(length = 200)
    private String mahsulotNomi;

    @Column(length = 60)
    private String mahsulotKod;

    /** Qaysi magazindan sotildi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazin_id")
    private Magazin magazin;

    /** Sotilgan miqdor (mahsulot birligida) */
    private Double miqdor;

    @Column(length = 20)
    private String birlik;

    /** 1 birlik uchun sotuv narxi (so'mda) */
    private Long birlikNarxi;

    /** Umumiy summa = miqdor × birlikNarxi (so'mda) */
    private Long summa;

    /** Sotilgan miqdorga to'g'ri keladigan zavod narxi (tannarx, so'mda) */
    private Long tannarx;

    /** foyda = summa − tannarx */
    private Long foyda;

    @Column(length = 150)
    private String mijozIsmi;

    @Column(length = 40)
    private String mijozTel;

    @Column(length = 500)
    private String izoh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sotgan_id", updatable = false)
    private Users sotgan;

    private LocalDateTime vaqt;

    @Column(length = 20)
    @Convert(converter = SotuvHolatiConverter.class)
    private SotuvHolati holat = SotuvHolati.SOTILDI;

    // ---- Qaytarish ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qaytargan_id")
    private Users qaytargan;

    private LocalDateTime qaytarilganVaqt;

    @Column(length = 500)
    private String qaytarishSababi;
}
