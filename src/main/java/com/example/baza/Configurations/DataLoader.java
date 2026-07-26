package com.example.baza.Configurations;

import com.example.baza.Entity.Users;
import com.example.baza.Repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if(usersRepository.count()==0){
            Users users = new Users();
            users.setUsername("owner");
            users.setRole("owner");
            users.setPassword(passwordEncoder.encode("1"));
            usersRepository.save(users);
        }
    }
}
