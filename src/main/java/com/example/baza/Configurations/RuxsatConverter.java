package com.example.baza.Configurations;

import com.example.baza.Entity.Ruxsat;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Ruxsat enum'ini bazaga oddiy MATN sifatida yozadi.
 *
 * NEGA KERAK: @Enumerated(EnumType.STRING) ishlatilsa, Hibernate jadval
 * yaratilayotganda ustunga CHECK constraint qo'yadi (o'sha paytdagi enum
 * qiymatlari ro'yxati bilan). Keyin enum'ga yangi ruxsat qo'shilsa,
 * ddl-auto=update bu constraint'ni YANGILAMAYDI va insert paytida
 * "rol_ruxsatlar_ruxsatlar_check" xatosi chiqadi.
 * Converter bilan Hibernate uchun bu oddiy String — CHECK constraint yaratilmaydi.
 *
 * Bazada noma'lum kod uchrasa (enum'dan olib tashlangan bo'lsa) null qaytadi —
 * shuning uchun o'qiydigan joylarda null filtri bor.
 */
@Converter
public class RuxsatConverter implements AttributeConverter<Ruxsat, String> {

    @Override
    public String convertToDatabaseColumn(Ruxsat ruxsat) {
        return ruxsat == null ? null : ruxsat.name();
    }

    @Override
    public Ruxsat convertToEntityAttribute(String kod) {
        if (kod == null || kod.isBlank()) return null;
        try {
            return Ruxsat.valueOf(kod.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
