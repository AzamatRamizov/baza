package com.example.baza.Dto;

import java.util.List;

public record UserDto(Long id,
                      String fish,
                      String tel,
                      String address,
                      String izoh,
                      String username,
                      List<RolQisqaDto> rollar,
                      Long menejerId,
                      String menejerFish) {
}
