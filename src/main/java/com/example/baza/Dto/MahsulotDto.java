package com.example.baza.Dto;

public record MahsulotDto(Long id,
                          String nomi,
                          Long kategoriyaId,
                          String kategoriyaNomi,
                          String birlik,
                          Long zavodNarxi,
                          Double boyi,
                          Double eni,
                          Double kv,
                          String turi,
                          Long magazinId,
                          String magazinNomi,
                          String yaratganFish) {
}
