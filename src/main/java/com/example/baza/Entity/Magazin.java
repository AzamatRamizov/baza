package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Magazin extends AbstractLongEntity {

    private String nomi;

    /**
     * Mas'ul hodimlar: bitta magazinga bir nechta hodim,
     * bitta hodim bir nechta magazinga mas'ul bo'lishi mumkin.
     * LAZY — heap muammosining oldini olish uchun (ro'yxat JOIN FETCH bilan olinadi).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "magazin_hodimlar",
            joinColumns = @JoinColumn(name = "magazin_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<Users> hodimlar = new LinkedHashSet<>();
}
