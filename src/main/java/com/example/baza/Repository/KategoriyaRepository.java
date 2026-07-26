package com.example.baza.Repository;

import com.example.baza.Entity.Kategoriya;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KategoriyaRepository extends JpaRepository<Kategoriya, Long> {

    Optional<Kategoriya> findByNomiIgnoreCase(String nomi);

    List<Kategoriya> findAllByOrderByNomiAsc();
}
