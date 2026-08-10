package com.example.baza.Configurations;

import com.example.baza.Entity.ArizaHolati;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Ariza holatini bazaga matn sifatida yozadi — SotuvHolatiConverter bilan bir
 * xil sabab: enum'ga yangi holat qo'shilganda CHECK constraint to'sqinlik qilmasin.
 */
@Converter
public class ArizaHolatiConverter implements AttributeConverter<ArizaHolati, String> {

    @Override
    public String convertToDatabaseColumn(ArizaHolati holat) {
        return holat == null ? null : holat.name();
    }

    @Override
    public ArizaHolati convertToEntityAttribute(String kod) {
        if (kod == null || kod.isBlank()) return null;
        try {
            return ArizaHolati.valueOf(kod.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
