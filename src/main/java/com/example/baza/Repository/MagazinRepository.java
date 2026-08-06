package com.example.baza.Repository;

import com.example.baza.Entity.Magazin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MagazinRepository extends JpaRepository<Magazin, Long> {

    @Query("select distinct m from Magazin m left join fetch m.hodimlar order by m.id")
    List<Magazin> findAllWithHodimlar();

    Optional<Magazin> findByNomiIgnoreCase(String nomi);

    /** Hodim o'chirilishidan oldin — u mas'ul bo'lgan magazinlarni topish uchun */
    List<Magazin> findByHodimlar_Id(Long hodimId);
}
