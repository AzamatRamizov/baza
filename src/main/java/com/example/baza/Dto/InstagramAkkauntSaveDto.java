package com.example.baza.Dto;

/** pageAccessToken bo'sh qoldirilsa (tahrirlashda) — mavjud token o'zgartirilmaydi */
public record InstagramAkkauntSaveDto(String nomi,
                                      String pageId,
                                      String pageAccessToken,
                                      Long masulUserId,
                                      boolean faol) {
}
