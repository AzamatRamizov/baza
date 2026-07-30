package com.example.baza.Dto;

import java.util.List;

public record UserAddDto(String fish,
                         String tel,
                         String address,
                         String izoh,
                         String username,
                         String password,
                         List<Long> rolIds,
                         Long menejerId) {
}
