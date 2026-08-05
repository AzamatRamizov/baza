package com.example.baza.Entity;

import com.example.baza.Configurations.AbstractLongEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Users extends AbstractLongEntity implements UserDetails {
    private String fish;
    private String tel;
    private String address;
    private String izoh;
    private String username;
    private String password;

    /**
     * Hodimning menejeri (masalan sotuvchiga biriktirilgan menejer).
     * LAZY — faqat kerak bo'lganda o'qiladi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menejer_id")
    private Users menejer;

    /**
     * Userning rollari — bitta odam 2-3 ta rolda bo'lishi mumkin.
     * EAGER ataylab: har so'rovda authorities (ruxsatlar) uchun kerak.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_rollar",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> rollar = new LinkedHashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (rollar == null || rollar.isEmpty()) return List.of();

        // Owner (tizim roli) bo'lsa — DOIM barcha ruxsatlar,
        // keyin qo'shilgan yangi Ruxsat'lar ham avtomatik kiradi
        boolean owner = rollar.stream().anyMatch(Rol::isTizimRoli);
        if (owner) {
            return Arrays.stream(Ruxsat.values())
                    .map(r -> new SimpleGrantedAuthority(r.name()))
                    .toList();
        }

        // Aks holda — barcha rollar ruxsatlarining birlashmasi
        return rollar.stream()
                .flatMap(rol -> rol.getRuxsatlar().stream())
                .filter(java.util.Objects::nonNull)   // bazadagi noma'lum kod -> null
                .distinct()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
