package com.example.baza.Dto;

import com.example.baza.Entity.ArizaHolati;

import java.time.LocalDateTime;

public record ArizaDto(Long id,
                       String ismFamiliya,
                       String telefon,
                       String manba,
                       String akkauntNomi,
                       String formId,
                       String adId,
                       ArizaHolati holat,
                       String holatNomi,
                       String izoh,
                       LocalDateTime createdTime) {
}
