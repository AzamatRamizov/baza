package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * Tizim sozlamalari — YAGONA qator (singleton: jadvalda doim faqat bitta yozuv
 * bo'ladi, {@link com.example.baza.Service.SozlamaService#olish} shuni ta'minlaydi).
 * Keyingi sozlamalar shu yerga yangi maydon sifatida qo'shib boriladi.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Sozlama extends AbstractLongEntity {

    /**
     * Kod (QR kod / shtrix-kod) "Chop etish" bosilganda bitta sahifada nechta
     * nusxada chiqarilsin — barcha "chop etish" tugmalari (/chop.js) shu qiymatni ishlatadi.
     */
    @ColumnDefault("4")
    private Integer chopEtishNusxaSoni = 4;
}
