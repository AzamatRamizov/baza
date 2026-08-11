package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import com.example.baza.Configurations.SotuvHolatiConverter;
import com.example.baza.Configurations.SotuvTuriConverter;
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
 *
 * METRAJ (kv.metr) mahsulotda sotuvchi METR kiritadi (eni o'zgarmaydi):
 *   3 m eni × 20 m bo'yi -> 60 kv.metr, narx ham 1 kv.metr uchun kiritiladi.
 *
 * KATM: mahsulot nasiyaga chiqarilganda holat KATMDA bo'ladi — mahsulot band
 * (qoldiqdan chiqadi), lekin tushum/foydaga KATM tasdiqlagandan keyin qo'shiladi.
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

    /** Sotilgan miqdor (mahsulot birligida: dona / metr / kv.metr) */
    private Double miqdor;

    @Column(length = 20)
    private String birlik;

    // ---- Metrlab sotuv (metraj mahsulotlar uchun) ----
    /** Kesib berilgan bo'yi (metr) */
    private Double boyi;

    /** Mahsulotning eni (metr) — o'zgarmaydi, faqat bo'yi qirqiladi */
    private Double eni;

    /** Sotilgan kvadrat: boyi × eni (kv.metr mahsulotlarda = miqdor) */
    private Double kv;

    /** 1 birlik uchun sotuv narxi (so'mda) — metrajda 1 KV.METR uchun */
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

    /** Nasiya muddati (oy) — KATM sotuvida */
    private Integer muddat;

    /** Oldindan (boshlang'ich naqd) to'lov — KATM sotuvida gilamga uzatiladi */
    private Long oldindanTulov;

    @Column(length = 500)
    private String izoh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sotgan_id", updatable = false)
    private Users sotgan;

    private LocalDateTime vaqt;

    @Column(length = 20)
    @Convert(converter = SotuvTuriConverter.class)
    private SotuvTuri turi = SotuvTuri.NAQD;

    @Column(length = 20)
    @Convert(converter = SotuvHolatiConverter.class)
    private SotuvHolati holat = SotuvHolati.SOTILDI;

    // ---- KATM ----
    /** KATMga o'tkazilgan vaqt */
    private LocalDateTime katmVaqti;

    /** So'rov gilam dasturiga yetib bordimi */
    private Boolean gilamgaYuborildi = false;

    /** Gilamdagi so'rov (KatmSorov) id'si — yuborilgandan keyin qaytadi */
    private Long gilamSorovId;

    /** Yuborishda xatolik bo'lsa — sababi (qayta yuborish uchun) */
    @Column(length = 300)
    private String gilamXato;

    /** KATM javobi (izoh: tasdiq sababi yoki rad etish sababi) */
    @Column(length = 500)
    private String katmJavobi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "katm_javob_bergan_id")
    private Users katmJavobBergan;

    private LocalDateTime katmJavobVaqti;

    // ---- Qaytarish ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qaytargan_id")
    private Users qaytargan;

    private LocalDateTime qaytarilganVaqt;

    @Column(length = 500)
    private String qaytarishSababi;

    // ---- Kassada qabul qilish (owner tomonidan naqd/tushum tasdiqlanishi) ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kassa_qabul_qilgan_id")
    private Users kassaQabulQilgan;

    private LocalDateTime kassaQabulVaqti;
}
