package com.example.baza.Repository;

import com.example.baza.Dto.OtkazmaDto;
import com.example.baza.Entity.Otkazma;
import com.example.baza.Entity.OtkazmaHolati;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OtkazmaRepository extends JpaRepository<Otkazma, Long> {

    String SELECT = """
            select new com.example.baza.Dto.OtkazmaDto(
                o.id, m.id, m.nomi, m.kod,
                qd.id, qd.nomi, qg.id, qg.nomi,
                o.miqdor, o.birlik, o.natijaMahsulotId,
                yu.fish, hq.fish,
                o.holat, o.izoh, o.javobIzoh,
                o.yuborilganVaqt, o.halQilinganVaqt)
            from Otkazma o
            join o.mahsulot m
            left join o.qayerdan qd
            left join o.qayerga qg
            left join o.yuborgan yu
            left join o.halQilgan hq
            """;

    /** Menga kelayotgan o'tkazmalar — men mas'ul bo'lgan magazinga jo'natilganlari */
    @Query(SELECT + """
            where o.holat = :holat
              and exists (select 1 from Magazin mg join mg.hodimlar h
                          where mg.id = qg.id and h.id = :userId)
            order by o.id desc
            """)
    List<OtkazmaDto> findKelayotganlar(@Param("userId") Long userId,
                                       @Param("holat") OtkazmaHolati holat);

    /** Owner uchun — holat bo'yicha barchasi */
    @Query(SELECT + " where o.holat = :holat order by o.id desc")
    List<OtkazmaDto> findHolatBoyicha(@Param("holat") OtkazmaHolati holat);

    /** Men jo'natganlar (barcha holatlar) */
    @Query(SELECT + " where yu.id = :userId order by o.id desc")
    List<OtkazmaDto> findMenYuborganlar(@Param("userId") Long userId);

    /** Owner uchun — barcha o'tkazmalar */
    @Query(SELECT + " order by o.id desc")
    List<OtkazmaDto> findHammasi();

    /** Bitta mahsulot bo'yicha o'tkazmalar tarixi */
    @Query(SELECT + " where m.id = :mahsulotId order by o.id desc")
    List<OtkazmaDto> findMahsulotBoyicha(@Param("mahsulotId") Long mahsulotId);

    /** Shu mahsulot bo'yicha tasdiqlanmagan o'tkazma bormi */
    boolean existsByMahsulot_IdAndHolat(Long mahsulotId, OtkazmaHolati holat);
}