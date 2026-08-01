package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
//import com.example.baza.Configurations.OtkazmaHolatiConverter;
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
 * Mahsulotni bir magazindan boshqasiga o'tkazish so'rovi.
 *
 * Oqim:  jo'natildi (KUTILMOQDA) -> qabul qiluvchi magazin hodimi
 *        tasdiqlaydi (QABUL_QILINDI, mahsulot magazini o'zgaradi)
 *        yoki rad etadi (RAD_ETILDI, mahsulot joyida qoladi).
 *        Jo'natgan odam tasdiqlanmagunicha bekor qila oladi (BEKOR_QILINDI).
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Otkazma extends AbstractLongEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mahsulot_id")
    private Mahsulot mahsulot;

    /** Qaysi magazindan (mahsulotning jo'natish paytidagi magazini) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qayerdan_id")
    private Magazin qayerdan;

    /** Qaysi magazinga */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qayerga_id")
    private Magazin qayerga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yuborgan_id", updatable = false)
    private Users yuborgan;

    /** Tasdiqlagan yoki rad etgan hodim */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hal_qilgan_id")
    private Users halQilgan;

    /** @Enumerated emas — CHECK constraint muammosi uchun, OtkazmaHolatiConverter'ga qarang */
    @Column(length = 20)
//    @Convert(converter = OtkazmaHolatiConverter.class)
    private OtkazmaHolati holat = OtkazmaHolati.KUTILMOQDA;

    /**
     * Jo'natilayotgan miqdor (mahsulotning birligida).
     * Mahsulotning butun qoldig'i jo'natilsa — mahsulotning o'zi ko'chadi;
     * qismi jo'natilsa (kesib berilsa) — qabul qilinganda yangi mahsulot ochiladi.
     */
    private Double miqdor;

    /** Jo'natish paytidagi birlik nusxasi (dona / metr / kv.metr) */
    @Column(length = 20)
    private String birlik;

    /** Qisman jo'natishda paydo bo'lgan yangi mahsulot id'si (bo'lmasa null) */
    private Long natijaMahsulotId;

    /** Jo'natuvchining izohi */
    @Column(length = 500)
    private String izoh;

    /** Qabul qiluvchining javobi (ayniqsa rad etish sababi) */
    @Column(length = 500)
    private String javobIzoh;

    private LocalDateTime yuborilganVaqt;

    private LocalDateTime halQilinganVaqt;
}