package com.example.baza.Dto;

public record MahsulotDto(Long id,
                          String nomi,
                          String kod,
                          Long kategoriyaId,
                          String kategoriyaNomi,
                          String birlik,
                          Long zavodNarxi,
                          Double boyi,
                          Double eni,
                          Double kv,
                          Double miqdor,
                          String turi,
                          Long magazinId,
                          String magazinNomi,
                          String yaratganFish,
                          String rasm) {
}