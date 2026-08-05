package com.example.baza.Configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Eski enum CHECK constraint'larini bazadan tozalaydi.
 *
 * MUAMMO: ilgari enum maydonlar @Enumerated(EnumType.STRING) bilan mapping
 * qilingan edi. Hibernate jadvalni YARATAYOTGANDA ustunga
 *     check (ruxsatlar = any (array['HODIM_BOSHQARISH'::text, ...]))
 * ko'rinishidagi constraint qo'shadi. Keyin enum'ga yangi qiymat qo'shilsa
 * (masalan SOTUV_KORISH, KATM_OTKAZISH), ddl-auto=update bu constraint'ni
 * YANGILAMAYDI — natijada insert paytida:
 *     "новая строка ... нарушает ограничение-проверку rol_ruxsatlar_ruxsatlar_check"
 *
 * YECHIM: enum maydonlar AttributeConverter'ga o'tkazildi (RuxsatConverter,
 * SotuvHolatiConverter, SotuvTuriConverter, OtkazmaHolatiConverter) — endi
 * Hibernate yangi constraint yaratmaydi. Bazada QOLIB KETGAN eskilarini esa
 * shu klass ishga tushishda bir marta o'chirib yuboradi (bazani qaytadan
 * yaratish shart emas, ma'lumotlar saqlanadi).
 *
 * Faqat Hibernate qo'ygan "= any (array[...::text])" ko'rinishidagi CHECK'lar
 * o'chiriladi — boshqa constraint'larga (FK, unique, not null) tegilmaydi.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // DataLoader'dan OLDIN ishlashi kerak
public class CheckConstraintTozalash implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CheckConstraintTozalash.class);

    private static final String QIDIRUV = """
            select conrelid::regclass::text as jadval, conname as nomi
            from pg_constraint
            where contype = 'c'
              and connamespace = 'public'::regnamespace
              and lower(pg_get_constraintdef(oid)) like '%= any (array[%'
              and lower(pg_get_constraintdef(oid)) like '%::text%'
            """;

    private final JdbcTemplate jdbcTemplate;

    public CheckConstraintTozalash(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Map<String, Object>> royxat = jdbcTemplate.queryForList(QIDIRUV);
            if (royxat.isEmpty()) return;

            for (Map<String, Object> qator : royxat) {
                String jadval = String.valueOf(qator.get("jadval"));
                String nomi = String.valueOf(qator.get("nomi"));
                try {
                    jdbcTemplate.execute("alter table " + jadval +
                            " drop constraint if exists \"" + nomi + "\"");
                    log.info("Eski enum CHECK constraint o'chirildi: {}.{}", jadval, nomi);
                } catch (Exception e) {
                    log.warn("Constraint o'chirilmadi ({}.{}): {}", jadval, nomi, e.getMessage());
                }
            }
        } catch (Exception e) {
            // PostgreSQL bo'lmasa yoki huquq yetmasa — dastur baribir ishlayversin
            log.warn("CHECK constraint tozalash o'tkazib yuborildi: {}", e.getMessage());
        }
    }
}
