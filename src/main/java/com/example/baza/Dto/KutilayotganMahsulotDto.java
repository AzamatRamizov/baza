package com.example.baza.Dto;

import java.time.LocalDateTime;

public record KutilayotganMahsulotDto(Long id,
                                      String nomi,
                                      String kod,
                                      String serialKod,
                                      Double boyi,
                                      Double eni,
                                      Double kv,
                                      Double miqdor,
                                      String turi,
                                      String holat,
                                      Boolean skanerlandi,
                                      String skanerlaganFish,
                                      LocalDateTime skanerlanganVaqt,
                                      String yuklaganFish,
                                      LocalDateTime yuklanganVaqt,
                                      String faylNomi) {
}
