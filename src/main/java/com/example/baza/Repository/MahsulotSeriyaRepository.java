package com.example.baza.Repository;

import com.example.baza.Entity.MahsulotSeriya;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MahsulotSeriyaRepository extends JpaRepository<MahsulotSeriya, Long> {

    List<MahsulotSeriya> findByMahsulot_IdOrderByIdAsc(Long mahsulotId);

    /** Universal skaner uchun — seriya raqami bo'yicha qaysi mahsulot(lar)ga tegishli */
    List<MahsulotSeriya> findBySeriyaKodIgnoreCase(String seriyaKod);

    boolean existsBySeriyaKodIgnoreCase(String seriyaKod);

    boolean existsByMahsulot_IdAndSeriyaKodIgnoreCase(Long mahsulotId, String seriyaKod);

    void deleteByMahsulot_Id(Long mahsulotId);
}
