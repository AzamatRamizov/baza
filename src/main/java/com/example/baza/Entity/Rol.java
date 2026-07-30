package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Rol extends AbstractLongEntity {

    private String nomi;

    /**
     * Tizim roli (Owner) — o'chirilmaydi, nomi o'zgartirilmaydi,
     * DOIM barcha ruxsatlarga ega (ruxsatlari ham tahrirlanmaydi).
     */
    private boolean tizimRoli;

    /**
     * Bu roldagi hodimga menejer biriktirilishi kerakmi
     * (masalan Sotuvchi roli uchun yoqiladi).
     */
    private boolean menejerKerak;

    /**
     * Rolning ruxsatlari. EAGER ataylab — har bir so'rovda autentifikatsiya
     * uchun kerak bo'ladi va hajmi juda kichik (bir necha satr).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_ruxsatlar", joinColumns = @JoinColumn(name = "rol_id"))
    @Enumerated(EnumType.STRING)
    private Set<Ruxsat> ruxsatlar = new LinkedHashSet<>();
}
