package com.example.baza.Dto;

import com.example.baza.Entity.SotuvHolati;
import com.example.baza.Entity.SotuvTuri;

import java.time.LocalDateTime;

public record SotuvDto(Long id,
                       Long mahsulotId,
                       String mahsulotNomi,
                       String mahsulotKod,
                       Long magazinId,
                       String magazinNomi,
                       Double miqdor,
                       String birlik,
                       Double boyi,
                       Double eni,
                       Double kv,
                       Long birlikNarxi,
                       Long summa,
                       Long tannarx,
                       Long foyda,
                       String mijozIsmi,
                       String mijozTel,
                       Integer muddat,
                       Long oldindanTulov,
                       String izoh,
                       String sotganFish,
                       LocalDateTime vaqt,
                       SotuvTuri turi,
                       SotuvHolati holat,
                       LocalDateTime katmVaqti,
                       Boolean gilamgaYuborildi,
                       String gilamXato,
                       String katmJavobi,
                       String katmJavobBerganFish,
                       LocalDateTime katmJavobVaqti,
                       String qaytarganFish,
                       LocalDateTime qaytarilganVaqt,
                       String qaytarishSababi,
                       String kassaQabulQilganFish,
                       LocalDateTime kassaQabulVaqti) {
}
