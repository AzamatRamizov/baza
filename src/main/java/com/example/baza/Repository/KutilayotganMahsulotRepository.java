package com.example.baza.Repository;

import com.example.baza.Entity.KutilayotganMahsulot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KutilayotganMahsulotRepository extends JpaRepository<KutilayotganMahsulot, Long> {

    List<KutilayotganMahsulot> findByHolatOrderByIdDesc(String holat);

    List<KutilayotganMahsulot> findByHolatAndSerialKodIgnoreCase(String holat, String serialKod);

    boolean existsByHolatAndSerialKodIgnoreCase(String holat, String serialKod);
}
