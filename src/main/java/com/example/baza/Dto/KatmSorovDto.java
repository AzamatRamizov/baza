package com.example.baza.Dto;

import java.time.LocalDateTime;

/**
 * Gilam dasturiga yuboriladigan KATM so'rovi.
 * Gilamda shu ma'lumotlar "Yangi shartnoma" formasiga to'ldiriladi.
 */
public record KatmSorovDto(Long sotuvId,
                           String mijozIsmi,
                           String mijozTel,
                           String mijozJshshir,
                           String mahsulotNomi,
                           String mahsulotKod,
                           Double miqdor,
                           String birlik,
                           Double boyi,
                           Double eni,
                           Double kv,
                           Long tannarx,
                           Long sotuvNarxi,
                           Long birlikNarxi,
                           Long oldindanTulov,
                           Integer muddat,
                           String dokonNomi,
                           String izoh,
                           String sotuvchi,
                           LocalDateTime vaqt) {
}
