package com.example.baza.Dto;

import java.time.LocalDateTime;

public record TarixDto(Long id,
                       LocalDateTime vaqt,
                       String username,
                       String fish,
                       String bolim,
                       String amal,
                       Long obyektId,
                       String obyektNomi,
                       String tafsilot) {
}
