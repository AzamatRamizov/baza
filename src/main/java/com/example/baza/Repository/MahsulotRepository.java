package com.example.baza.Repository;

import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Entity.Mahsulot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
                m.boyi, m.eni, m.kv, m.miqdor, m.turi,
                mag.id, mag.nomi, u.fish, m.rasm, true, 0L)
            from Mahsulot m
            left join m.kategoriya k
            left join m.magazin mag
            left join m.yaratganUser u
            order by m.id desc
            """)
    List<MahsulotDto> findAllDto();

    /** Bitta mahsulot — detail sahifasi uchun (o'chirilib bo'lmaydigan lazy bog'lanishlarsiz) */
    @Query("""
            select new com.example.baza.Dto.MahsulotDto(
                m.id, m.nomi, m.kod, k.id, k.nomi, m.birlik, m.zavodNarxi,
                m.boyi, m.eni, m.kv, m.miqdor, m.turi,
                mag.id, mag.nomi, u.fish, m.rasm, true, 0L)
            from Mahsulot m
            left join m.kategoriya k
            left join m.magazin mag
            left join m.yaratganUser u
            where m.id = :id
            """)
    Optional<MahsulotDto> findDtoById(@Param("id") Long id);

    /**
     * "Mening mahsulotlarim" — men mas'ul bo'lgan magazin(lar)dagi mahsulotlar.
     * Bitta hodim bir nechta magazinga mas'ul bo'lishi mumkin.
     */
    @Query("""
            select distinct new com.example.baza.Dto.MahsulotDto(
                m.id, m.nomi, m.kod, k.id, k.nomi, m.birlik, m.zavodNarxi,
                m.boyi, m.eni, m.kv, m.miqdor, m.turi,
                mag.id, mag.nomi, u.fish, m.rasm, true, 0L)
            from Mahsulot m
            left join m.kategoriya k
            join m.magazin mag
            join mag.hodimlar h
            left join m.yaratganUser u
            where h.id = :userId
            order by m.id desc
            """)
    List<MahsulotDto> findMagazinimDto(@Param("userId") Long userId);

    /** Kategoriya o'chirilishidan oldin tekshirish uchun */
    long countByKategoriya_Id(Long kategoriyaId);

    /**
     * Bitta kod bilan bir nechta mahsulot bo'lishi mumkin (ommaviy import bir xil
     * dizayn kodini turli o'lchamlarda ataylab takrorlaydi) — shuning uchun List,
     * Optional/single-result EMAS (aks holda NonUniqueResultException beradi).
     */
    List<Mahsulot> findByKodIgnoreCase(String kod);

    /** Skanerlangan mahsulot shu hodimning magazin(lar)iga tegishlimi — "Mening mahsulotlarim" bilan bir xil scoping */
    boolean existsByIdAndMagazin_Hodimlar_Id(Long mahsulotId, Long userId);

    /** Ommaviy import vaqtida takroriy kodlarni tekshirish uchun — to'liq entity yuklanmaydi */
    @Query("select m.kod from Mahsulot m where m.kod is not null")
    List<String> findAllKodlar();

    /** Hodim o'chirilganda — "kim yaratgan" izini bog'lanishdan bo'shatadi */
    @Modifying
    @Query("update Mahsulot m set m.yaratganUser = null where m.yaratganUser.id = :userId")
    void yaratganUserniTozalash(@Param("userId") Long userId);
}