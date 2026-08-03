package com.example.baza.Dto;

import com.example.baza.Entity.SotuvHolati;

import java.time.LocalDateTime;

public record SotuvDto(Long id,
                       Long mahsulotId,
                       String mahsulotNomi,
                       String mahsulotKod,
                       Long magazinId,
                       String magazinNomi,
                       Double miqdor,
                       String birlik,
                       Long birlikNarxi,
                       Long summa,
                       Long tannarx,
                       Long foyda,
                       String mijozIsmi,
                       String mijozTel,
                       String izoh,
                       String sotganFish,
                       LocalDateTime vaqt,
                       SotuvHolati holat,
                       String qaytarganFish,
                       LocalDateTime qaytarilganVaqt,
                       String qaytarishSababi) {
}
