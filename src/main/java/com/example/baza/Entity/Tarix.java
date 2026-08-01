package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tizimdagi barcha amallar tarixi (audit log).
 * Yozuvlar faqat qo'shiladi — tahrirlanmaydi va o'chirilmaydi.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tarix", indexes = {
        @Index(name = "idx_tarix_vaqt", columnList = "vaqt"),
        @Index(name = "idx_tarix_bolim", columnList = "bolim")
})
public class Tarix extends AbstractLongEntity {

    private LocalDateTime vaqt;

    /** Bo'lim: Mahsulot, Magazin, Hodim, Rol, Kategoriya, O'tkazma, Kirish */
    @Column(length = 40)
    private String bolim;

    /** Amal nomi: "Qo'shildi", "Tahrirlandi", "O'chirildi", "Tasdiqlandi" ... */
    @Column(length = 60)
    private String amal;

    /** Qaysi yozuv ustida — id va nomi (nomi keyin o'chib ketsa ham qoladi) */
    private Long obyektId;

    @Column(length = 200)
    private String obyektNomi;

    /** Qo'shimcha tafsilot (nima o'zgardi) */
    @Column(length = 1500)
    private String tafsilot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    /** Username nusxasi — hodim o'chirilsa ham tarixda ko'rinib tursin */
    @Column(length = 100)
    private String username;
}
