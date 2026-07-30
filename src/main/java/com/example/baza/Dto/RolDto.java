package com.example.baza.Dto;

import java.util.List;

public record RolDto(Long id,
                     String nomi,
                     List<String> ruxsatlar,
                     long userSoni,
                     boolean tizimRoli,
                     boolean menejerKerak) {
}
