package com.example.baza.Repository;

import com.example.baza.Dto.SotuvDto;
import com.example.baza.Entity.Sotuv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SotuvRepository extends JpaRepository<Sotuv, Long> {

    String SELECT = """
            select new com.example.baza.Dto.SotuvDto(
                s.id, m.id, s.mahsulotNomi, s.mahsulotKod,
                mag.id, mag.nomi,
                s.miqdor, s.birlik, s.boyi, s.eni, s.kv,
                s.birlikNarxi, s.summa, s.tannarx, s.foyda,
                s.mijozIsmi, s.mijozTel, s.mijozJshshir, s.muddat, s.oldindanTulov, s.izoh,
                so.fish, s.vaqt, s.turi, s.holat,
                s.katmVaqti, s.gilamgaYuborildi, s.gilamXato,
                s.katmJavobi, kj.fish, s.katmJavobVaqti,
                qa.fish, s.qaytarilganVaqt, s.qaytarishSababi)
            from Sotuv s
            left join s.mahsulot m
            left join s.magazin mag
            left join s.sotgan so
            left join s.katmJavobBergan kj
            left join s.qaytargan qa
            """;

    /** Men mas'ul bo'lgan magazin(lar)dagi sotuvlar */
    @Query(SELECT + """
            where s.vaqt >= :sanadan and s.vaqt <= :sanagacha
              and exists (select 1 from Magazin mg join mg.hodimlar h
                          where mg.id = mag.id and h.id = :userId)
            order by s.id desc
            """)
    List<SotuvDto> findMeniki(@Param("userId") Long userId,
                              @Param("sanadan") LocalDateTime sanadan,
                              @Param("sanagacha") LocalDateTime sanagacha);

    /** Owner uchun — barcha magazinlar */
    @Query(SELECT + """
            where s.vaqt >= :sanadan and s.vaqt <= :sanagacha
            order by s.id desc
            """)
    List<SotuvDto> findHammasi(@Param("sanadan") LocalDateTime sanadan,
                               @Param("sanagacha") LocalDateTime sanagacha);

    /** Bitta mahsulotning sotuv tarixi */
    @Query(SELECT + " where m.id = :mahsulotId order by s.id desc")
    List<SotuvDto> findMahsulotBoyicha(@Param("mahsulotId") Long mahsulotId);
}
