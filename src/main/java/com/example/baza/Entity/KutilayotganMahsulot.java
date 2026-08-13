package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Excel orqali yuklangan, lekin hali mahsulotga TASDIQLANMAGAN qator.
 *
 * OQIM: Excel yuklanadi -> har bir qator shu jadvalga "KUTILMOQDA" holatida yoziladi
 * (Mahsulot jadvaliga TEGMAYDI). Hodim fizik mahsulotni shtrix-kod skaneri bilan
 * o'qib, seriya raqami mos kelgan qatorni "skanerlandi" deb belgilaydi. Keyin
 * "Tasdiqlash" tugmasi bosilganda — FAQAT skanerlangan qatorlar haqiqiy Mahsulot
 * sifatida qo'shiladi (magazin shu bosishda tanlanadi), holat "TASDIQLANDI"ga o'tadi.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class KutilayotganMahsulot extends AbstractLongEntity {

    private String nomi;
    private String kod;
    private String serialKod;

    /** O'lchamlar (metrda) */
    private Double boyi;
    private Double eni;
    private Double kv;

    /** Dona soni */
    private Double miqdor;

    /** "gatoviy" yoki "metraj" */
    private String turi;

    /** "KUTILMOQDA" | "TASDIQLANDI" */
    private String holat = "KUTILMOQDA";

    private Boolean skanerlandi = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skanerlagan_id")
    private Users skanerlagan;

    private LocalDateTime skanerlanganVaqt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yuklagan_id")
    private Users yuklagan;

    private LocalDateTime yuklanganVaqt;

    /** Tasdiqlanganda qaysi magazinga qo'shilgani (tarix uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazin_id")
    private Magazin magazin;

    @Column(length = 200)
    private String faylNomi;
}
