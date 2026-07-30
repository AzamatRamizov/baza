package com.example.baza.Repository;

import com.example.baza.Entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNomiIgnoreCase(String nomi);

    Optional<Rol> findByTizimRoliTrue();

    List<Rol> findAllByOrderByNomiAsc();
}
