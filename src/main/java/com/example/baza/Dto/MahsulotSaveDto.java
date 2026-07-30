package com.example.baza.Dto;

public record MahsulotSaveDto(String nomi,
                              String kod,
                              Long kategoriyaId,
                              String birlik,
                              Double zavodNarxi,
                              String valyuta,
                              Double boyi,
                              Double eni,
                              String turi,
                              Long magazinId) {
}
