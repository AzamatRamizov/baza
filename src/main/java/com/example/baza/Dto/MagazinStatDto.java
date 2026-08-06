package com.example.baza.Dto;

/** Bitta magazinning statistika sahifasidagi qatori */
public record MagazinStatDto(Long id,
                             String nomi,
                             long mahsulotSoni,
                             double umumiyKv,
                             long zavodQiymati,
                             long sotuvSoni,
                             long sotuvSummasi,
                             long foyda) {
}
