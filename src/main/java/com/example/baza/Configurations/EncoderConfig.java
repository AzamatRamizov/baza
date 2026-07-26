package com.example.baza.Configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder alohida konfiguratsiyada, chunki:
 * Config -> JwtFilter -> UserService -> PasswordEncoder -> Config
 * zanjiri siklik bog'liqlik hosil qilardi.
 */
@Configuration
public class EncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
