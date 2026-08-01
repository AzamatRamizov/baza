package com.example.baza.Dto;

/** Mahsulotni boshqa magazinga jo'natish so'rovi */
public record OtkazmaSaveDto(Long mahsulotId,
                             Long qayergaMagazinId,
                             Double miqdor,
                             String izoh) {
}