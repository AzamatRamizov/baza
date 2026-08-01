package com.example.baza.Dto;

import com.example.baza.Entity.OtkazmaHolati;

import java.time.LocalDateTime;

public record OtkazmaDto(Long id,
                         Long mahsulotId,
                         String mahsulotNomi,
                         String mahsulotKod,
                         Long qayerdanId,
                         String qayerdanNomi,
                         Long qayergaId,
                         String qayergaNomi,
                         Double miqdor,
                         String birlik,
                         Long natijaMahsulotId,
                         String yuborganFish,
                         String halQilganFish,
                         OtkazmaHolati holat,
                         String izoh,
                         String javobIzoh,
                         LocalDateTime yuborilganVaqt,
                         LocalDateTime halQilinganVaqt) {
}