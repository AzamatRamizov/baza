package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import com.example.baza.Configurations.ArizaHolatiConverter;
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

/**
 * Instagram (Meta Lead Ads) orqali kelgan ariza — reklama videosida
 * "telefon raqamingizni qoldiring" formasini to'ldirgan foydalanuvchi.
 *
 * Meta webhook faqat leadgen_id yuboradi, to'liq maydonlar (ism, telefon)
 * Graph API'dan alohida so'rov bilan olinadi (InstagramGraphService).
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Ariza extends AbstractLongEntity {

    @Column(length = 150)
    private String ismFamiliya;

    @Column(length = 40)
    private String telefon;

    /** Qaysi manbadan kelgani — hozircha doim "Instagram" */
    @Column(length = 60)
    private String manba = "Instagram";

    /** Qaysi Instagram/Page akkauntidan kelgani — mas'ul xodim shu orqali aniqlanadi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instagram_akkaunt_id")
    private InstagramAkkaunt instagramAkkaunt;

    /** Meta leadgen_id — bitta arizani ikki marta saqlamaslik uchun unique */
    @Column(length = 100, unique = true)
    private String leadgenId;

    @Column(length = 60)
    private String formId;

    @Column(length = 60)
    private String adId;

    /** Graph API'dan kelgan xom field_data — kelajakda kerak bo'lsa qarash uchun */
    @Column(columnDefinition = "TEXT")
    private String xomMalumot;

    @Column(length = 20)
    @Convert(converter = ArizaHolatiConverter.class)
    private ArizaHolati holat = ArizaHolati.YANGI;

    @Column(length = 500)
    private String izoh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holat_ozgartirgan_id")
    private Users holatOzgartirgan;
}
