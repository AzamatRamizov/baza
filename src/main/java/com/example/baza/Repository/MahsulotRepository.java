package com.example.baza.Repository;

import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Entity.Mahsulot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MahsulotRepository extends JpaRepository<Mahsulot, Long> {

    /**
     * Konstruktor proyeksiya — lazy magazin/user'ga tegmasdan
     * kerakli maydonlarni bitta so'rovda oladi (heap-safe).
     */
    @Query("""
            select new com.example.baza.Dto.MahsulotDto(
                m.id, m.nomi, m.kod, k.id, k.nomi, m.birlik, m.zavodNarxi,
                m.boyi, m.eni, m.kv, m.turi,
                mag.id, mag.nomi, u.fish)
            from Mahsulot m
            left join m.kategoriya k
            left join m.magazin mag
            left join m.yaratganUser u
            order by m.id desc
            """)
    List<MahsulotDto> findAllDto();

    /** Kategoriya o'chirilishidan oldin tekshirish uchun */
    long countByKategoriya_Id(Long kategoriyaId);

    /** Kod takrorlanmasligini tekshirish uchun */
    Optional<Mahsulot> findByKodIgnoreCase(String kod);
}
