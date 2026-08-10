package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Bitta Instagram/Facebook Page — Lead Ads arizalarini shu akkaunt nomidan
 * qabul qilamiz. Bitta Meta App (App Secret, Verify Token — application.properties'da,
 * hammasi uchun umumiy) bir nechta Page'ni boshqarishi mumkin, lekin har bir
 * Page'ning o'z Access Token'i bor — shuning uchun u shu yerda, bazada saqlanadi.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class InstagramAkkaunt extends AbstractLongEntity {

    @Column(length = 150)
    private String nomi;

    /** Facebook Page ID — webhook kelganda entry.id shu bilan solishtiriladi */
    @Column(length = 60, unique = true)
    private String pageId;

    /** Uzoq muddatli Page Access Token — Graph API'dan lead ma'lumotini olish uchun */
    @Column(length = 500)
    private String pageAccessToken;

    /** Shu akkauntdan kelgan arizalarga mas'ul xodim */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "masul_user_id")
    private Users masulUser;

    private boolean faol = true;
}
