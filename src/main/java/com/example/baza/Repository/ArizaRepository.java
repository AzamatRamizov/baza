package com.example.baza.Repository;

import com.example.baza.Entity.Ariza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArizaRepository extends JpaRepository<Ariza, Long> {

    Optional<Ariza> findByLeadgenId(String leadgenId);

    List<Ariza> findAllByOrderByCreatedTimeDesc();

    List<Ariza> findAllByInstagramAkkaunt_MasulUser_UsernameOrderByCreatedTimeDesc(String username);
}
