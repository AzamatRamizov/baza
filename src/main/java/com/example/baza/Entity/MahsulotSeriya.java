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

/**
 * Bitta mahsulot (Mahsulot) qatoriga tegishli jismoniy dona(lar)ning zavod
 * seriya raqami. Bitta Mahsulot qatori BIR NECHTA seriyaga ega bo'lishi mumkin —
 * kodi/o'lchami bir xil bo'lgan yangi dona qo'shilganda (miqdorga qo'shish),
 * eski seriyalar yo'qolmaydi, yangisi ro'yxatga qo'shiladi.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class MahsulotSeriya extends AbstractLongEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mahsulot_id")
    private Mahsulot mahsulot;

    @Column(length = 255)
    private String seriyaKod;
}
