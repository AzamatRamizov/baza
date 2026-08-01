package com.example.baza.Repository;

import com.example.baza.Dto.TarixDto;
import com.example.baza.Entity.Tarix;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TarixRepository extends JpaRepository<Tarix, Long> {

    /**
     * Filtrlar ixtiyoriy — bo'sh qiymat "filtrsiz" degani:
     *   bolim = "", userId = -1, q = ""
     *
     * MUHIM: sanadan/sanagacha HECH QACHON null bo'lmasligi kerak.
     * PostgreSQL "(:param is null or ...)" ko'rinishidagi shartda parametr
     * turini aniqlay olmaydi va "не удалось определить тип данных параметра"
     * xatosini beradi. Shuning uchun filtr bo'sh bo'lganda TarixService
     * juda keng chegara (SANA_MIN / SANA_MAX) uzatadi.
     */
    @Query(value = """
            select new com.example.baza.Dto.TarixDto(
                t.id, t.vaqt, t.username, u.fish,
                t.bolim, t.amal, t.obyektId, t.obyektNomi, t.tafsilot)
            from Tarix t
            left join t.user u
            where (:bolim = '' or t.bolim = :bolim)
              and (:userId = -1 or u.id = :userId)
              and t.vaqt >= :sanadan
              and t.vaqt <= :sanagacha
              and (:q = ''
                   or lower(coalesce(t.obyektNomi, '')) like concat('%', :q, '%')
                   or lower(coalesce(t.tafsilot, '')) like concat('%', :q, '%')
                   or lower(coalesce(t.amal, '')) like concat('%', :q, '%')
                   or lower(coalesce(t.username, '')) like concat('%', :q, '%'))
            order by t.id desc
            """,
            countQuery = """
            select count(t)
            from Tarix t
            left join t.user u
            where (:bolim = '' or t.bolim = :bolim)
              and (:userId = -1 or u.id = :userId)
              and t.vaqt >= :sanadan
              and t.vaqt <= :sanagacha
              and (:q = ''
                   or lower(coalesce(t.obyektNomi, '')) like concat('%', :q, '%')
                   or lower(coalesce(t.tafsilot, '')) like concat('%', :q, '%')
                   or lower(coalesce(t.amal, '')) like concat('%', :q, '%')
                   or lower(coalesce(t.username, '')) like concat('%', :q, '%'))
            """)
    Page<TarixDto> qidir(@Param("bolim") String bolim,
                         @Param("userId") Long userId,
                         @Param("q") String q,
                         @Param("sanadan") LocalDateTime sanadan,
                         @Param("sanagacha") LocalDateTime sanagacha,
                         Pageable pageable);

    /** Filtr select'i uchun — tarixda uchraydigan bo'limlar */
    @Query("select distinct t.bolim from Tarix t where t.bolim is not null order by t.bolim")
    List<String> findBolimlar();
}