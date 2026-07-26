package com.example.baza.Dto;

import java.util.List;

public record MagazinDto(Long id,
                         String nomi,
                         List<UserDto> hodimlar) {
}
