package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.ParolUpdateDto;
import com.example.baza.Dto.ProfilUpdateDto;
import com.example.baza.Dto.UserAddDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.UsersRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User yo'q"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

    // ================= USER BOSHQARUVI =================

    public List<UserDto> getAllUsers() {
        return usersRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getFish(), u.getTel(),
                        u.getAddress(), u.getIzoh(), u.getUsername(), u.getRole()))
                .toList();
    }

    @Transactional
    public ApiResponse addUser(UserAddDto dto) {
        if (dto.username() == null || dto.username().isBlank()) {
            return new ApiResponse("Username kiritilishi shart", false);
        }
        if (dto.password() == null || dto.password().isBlank()) {
            return new ApiResponse("Parol kiritilishi shart", false);
        }
        if (dto.role() == null || dto.role().isBlank()) {
            return new ApiResponse("Rol tanlanishi shart", false);
        }
        if (usersRepository.findByUsername(dto.username().trim()).isPresent()) {
            return new ApiResponse("Bunday username allaqachon mavjud", false);
        }

        Users user = new Users();
        user.setFish(dto.fish());
        user.setTel(dto.tel());
        user.setAddress(dto.address());
        user.setIzoh(dto.izoh());
        user.setUsername(dto.username().trim());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        usersRepository.save(user);

        return new ApiResponse("Hodim qo'shildi", true);
    }

    // ================= PROFIL =================

    public UserDto getProfil(String username) {
        Users u = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User yo'q"));
        return new UserDto(u.getId(), u.getFish(), u.getTel(),
                u.getAddress(), u.getIzoh(), u.getUsername(), u.getRole());
    }

    @Transactional
    public ApiResponse updateProfil(String username, ProfilUpdateDto dto) {
        Users user = usersRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return new ApiResponse("Foydalanuvchi topilmadi", false);
        }

        user.setFish(dto.fish());
        user.setTel(dto.tel());
        user.setAddress(dto.address());
        user.setIzoh(dto.izoh());
        usersRepository.save(user);

        return new ApiResponse("Profil yangilandi", true);
    }

    @Transactional
    public ApiResponse updateParol(String username, ParolUpdateDto dto) {
        Users user = usersRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return new ApiResponse("Foydalanuvchi topilmadi", false);
        }
        if (dto.eskiParol() == null || dto.yangiParol() == null || dto.yangiParol().isBlank()) {
            return new ApiResponse("Eski va yangi parol kiritilishi shart", false);
        }
        if (!passwordEncoder.matches(dto.eskiParol(), user.getPassword())) {
            return new ApiResponse("Eski parol noto'g'ri", false);
        }

        user.setPassword(passwordEncoder.encode(dto.yangiParol()));
        usersRepository.save(user);

        return new ApiResponse("Parol yangilandi", true);
    }

    @Transactional
    public ApiResponse deleteUser(Long id) {
        Users user = usersRepository.findById(id).orElse(null);
        if (user == null) {
            return new ApiResponse("Hodim topilmadi", false);
        }

        // O'zini o'zi o'chirishga ruxsat yo'q
        String currentUsername = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            return new ApiResponse("O'zingizni o'chira olmaysiz", false);
        }

        usersRepository.delete(user);
        return new ApiResponse("Hodim o'chirildi", true);
    }
}
