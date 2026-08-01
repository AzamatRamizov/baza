package com.example.baza.Dto;

import java.util.List;

/** Tarix sahifasi (pagination) */
public record TarixSahifaDto(List<TarixDto> royxat,
                             int sahifa,
                             int jamiSahifa,
                             long jami) {
}
