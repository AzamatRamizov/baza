package com.example.baza.Configurations;

import com.example.baza.Entity.SotuvHolati;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Sotuv holatini bazaga matn sifatida yozadi — RuxsatConverter bilan bir xil
 * sabab: enum'ga yangi holat qo'shilganda CHECK constraint to'sqinlik qilmasin.
 */
@Converter
public class SotuvHolatiConverter implements AttributeConverter<SotuvHolati, String> {

    @Override
    public String convertToDatabaseColumn(SotuvHolati holat) {
        return holat == null ? null : holat.name();
    }

    @Override
    public SotuvHolati convertToEntityAttribute(String kod) {
        if (kod == null || kod.isBlank()) return null;
        try {
            return SotuvHolati.valueOf(kod.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
