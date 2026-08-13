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

    /**
     * Zavod (ishlab chiqaruvchi) tomonidan har bir jismoniy dona uchun berilgan
     * takrorlanmas seriya raqami — Excel orqali ommaviy import qilinganda kiritiladi.
     * Skaner bilan o'qilganda (agar QR/shtrix-kod havola emas, oddiy raqam bo'lsa)
     * shu maydon bo'yicha ham qidiriladi (kod bo'yicha topilmasa).
     */
    private String serialKod;

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

    /**
     * Mavjud (qoldiq) miqdor — mahsulotning BIRLIGIDA o'lchanadi:
     *   dona     -> nechta dona
     *   metr     -> necha metr
     *   kv.metr  -> necha kv.metr
     * Boshqa magazinga qisman jo'natilganda shu qiymat kamayadi.
     */
    private Double miqdor;

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

    /** Yuklangan rasm fayl nomi (diskda saqlanadi, DBda faqat nomi) — bo'lmasa null */
    private String rasm;
}