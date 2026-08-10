package com.example.baza.Repository;

import com.example.baza.Entity.InstagramAkkaunt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstagramAkkauntRepository extends JpaRepository<InstagramAkkaunt, Long> {

    Optional<InstagramAkkaunt> findByPageId(String pageId);

    List<InstagramAkkaunt> findAllByOrderByNomiAsc();

    List<InstagramAkkaunt> findAllByMasulUser_UsernameOrderByNomiAsc(String username);
}
