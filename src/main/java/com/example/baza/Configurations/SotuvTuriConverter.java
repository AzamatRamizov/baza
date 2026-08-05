package com.example.baza.Configurations;

import com.example.baza.Entity.SotuvTuri;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Sotuv turini bazaga matn sifatida yozadi (SotuvHolatiConverter bilan bir xil sabab:
 * enum'ga yangi qiymat qo'shilganda CHECK constraint to'sqinlik qilmasin).
 */
@Converter
public class SotuvTuriConverter implements AttributeConverter<SotuvTuri, String> {

    @Override
    public String convertToDatabaseColumn(SotuvTuri turi) {
        return turi == null ? null : turi.name();
    }

    @Override
    public SotuvTuri convertToEntityAttribute(String kod) {
        if (kod == null || kod.isBlank()) return null;
        try {
            return SotuvTuri.valueOf(kod.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
