package com.example.baza.Configurations;

import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Ruxsat;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.RolRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UsersRepository usersRepository,
                      RolRepository rolRepository,
                      PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1) Eng katta rol — "Owner" (tizim roli): bo'lmasa yaratiladi.
        //    Ruxsatlari to'ldirib qo'yiladi, lekin baribir tizim roli sifatida
        //    doim barcha ruxsatlarga ega deb hisoblanadi (Users.getAuthorities).
        Rol ownerRol = rolRepository.findByTizimRoliTrue().orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNomi("Owner");
            rol.setTizimRoli(true);
            rol.setRuxsatlar(new LinkedHashSet<>(List.of(Ruxsat.values())));
            return rolRepository.save(rol);
        });

        // 2) Userlar bo'lmasa — owner/1 (Owner roli bilan) yaratiladi
        if (usersRepository.count() == 0) {
            Users users = new Users();
            users.setUsername("owner");
            users.getRollar().add(ownerRol);
            users.setPassword(passwordEncoder.encode("1"));
            usersRepository.save(users);
        } else {
            // 3) Rolsiz qolgan "owner" bo'lsa — Owner biriktiriladi
            //    (aks holda hech kim rol/ruxsat sozlay olmay qolib qoladi)
            usersRepository.findByUsername("owner").ifPresent(owner -> {
                if (owner.getRollar() == null || owner.getRollar().isEmpty()) {
                    owner.getRollar().add(ownerRol);
                    usersRepository.save(owner);
                }
            });
        }
    }
}
