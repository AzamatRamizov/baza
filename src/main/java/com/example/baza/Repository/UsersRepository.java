package com.example.baza.Repository;

import com.example.baza.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    long countByRollar_Id(Long rolId);

    /** Hodim o'chirilganda — u boshqalarga menejer bo'lgan bo'lsa, bog'lanishni bo'shatadi */
    @Modifying
    @Query("update Users u set u.menejer = null where u.menejer.id = :userId")
    void menejerlikniTozalash(@Param("userId") Long userId);
}
