package com.example.baza.Dto;

import java.util.List;

public record MagazinSaveDto(String nomi,
                             List<Long> hodimIds) {
}
