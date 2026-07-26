package com.example.baza.Dto;

public record MahsulotSaveDto(String nomi,
                              Long kategoriyaId,
                              String birlik,
                              Long zavodNarxi,
                              Double boyi,
                              Double eni,
                              String turi,
                              Long magazinId) {
}
