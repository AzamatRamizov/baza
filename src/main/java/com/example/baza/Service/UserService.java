package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.ParolUpdateDto;
import com.example.baza.Dto.ProfilUpdateDto;
import com.example.baza.Dto.UserAddDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.OtkazmaRepository;
import com.example.baza.Repository.RolRepository;
import com.example.baza.Repository.SotuvRepository;
import com.example.baza.Repository.TarixRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

import com.example.baza.Dto.RolQisqaDto;

@Service
public class UserService implements UserDetailsService {
    private final UsersRepository usersRepository;
    private final RolRepository rolRepository;
    private final MagazinRepository magazinRepository;
    private final MahsulotRepository mahsulotRepository;
    private final OtkazmaRepository otkazmaRepository;
    private final SotuvRepository sotuvRepository;
    private final TarixRepository tarixRepository;
    private final PasswordEncoder passwordEncoder;
    private final TarixService tarixService;

    public UserService(UsersRepository usersRepository,
                       RolRepository rolRepository,
                       MagazinRepository magazinRepository,
                       MahsulotRepository mahsulotRepository,
                       OtkazmaRepository otkazmaRepository,
                       SotuvRepository sotuvRepository,
                       TarixRepository tarixRepository,
                       PasswordEncoder passwordEncoder,
                       TarixService tarixService) {
        this.usersRepository = usersRepository;
        this.rolRepository = rolRepository;
        this.magazinRepository = magazinRepository;
        this.mahsulotRepository = mahsulotRepository;
        this.otkazmaRepository = otkazmaRepository;
        this.sotuvRepository = sotuvRepository;
        this.tarixRepository = tarixRepository;
        this.passwordEncoder = passwordEncoder;
        this.tarixService = tarixService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Users entity'ning o'zi UserDetails — authorities rolning ruxsatlaridan keladi
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User yo'q"));
    }

    // ================= USER BOSHQARUVI =================

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return usersRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private UserDto toDto(Users u) {
        return new UserDto(u.getId(), u.getFish(), u.getTel(),
                u.getAddress(), u.getIzoh(), u.getUsername(),
                u.getRollar().stream()
                        .map(r -> new RolQisqaDto(r.getId(), r.getNomi()))
                        .toList(),
                u.getMenejer() == null ? null : u.getMenejer().getId(),
                u.getMenejer() == null ? null
                        : (u.getMenejer().getFish() != null && !u.getMenejer().getFish().isBlank()
                           ? u.getMenejer().getFish() : u.getMenejer().getUsername()));
    }

    @Transactional
    public ApiResponse addUser(UserAddDto dto) {
        if (dto.username() == null || dto.username().isBlank()) {
            return new ApiResponse("Username kiritilishi shart", false);
        }
        if (dto.password() == null || dto.password().isBlank()) {
            return new ApiResponse("Parol kiritilishi shart", false);
        }
        if (dto.rolIds() == null || dto.rolIds().isEmpty()) {
            return new ApiResponse("Kamida bitta rol tanlanishi shart", false);
        }
        List<Rol> tanlanganRollar = rolRepository.findAllById(dto.rolIds());
        if (tanlanganRollar.isEmpty()) {
            return new ApiResponse("Tanlangan rollar topilmadi", false);
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
        user.setRollar(new LinkedHashSet<>(tanlanganRollar));
        if (dto.menejerId() != null) {
            user.setMenejer(usersRepository.findById(dto.menejerId()).orElse(null));
        }
        usersRepository.save(user);

        tarixService.yoz("Hodim", "Qo'shildi", user.getId(), kim(user),
                "Username: " + user.getUsername() + " | Rollar: " + rollarMatn(user));
        return new ApiResponse("Hodim qo'shildi", true);
    }

    /**
     * Userning rollarini to'liq almashtirish (userlar sahifasidagi
     * checkbox panelidan) — qo'shish/olib tashlash shu bitta amalda.
     */
    @Transactional
    public ApiResponse updateUserRollar(Long userId, List<Long> rolIds) {
        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ApiResponse("Hodim topilmadi", false);
        }

        String eski = rollarMatn(user);
        if (rolIds == null || rolIds.isEmpty()) {
            user.setRollar(new LinkedHashSet<>());
        } else {
            user.setRollar(new LinkedHashSet<>(rolRepository.findAllById(rolIds)));
        }
        usersRepository.save(user);

        tarixService.yoz("Hodim", "Rollari o'zgardi", user.getId(), kim(user),
                "Oldin: " + eski + " | Endi: " + rollarMatn(user));
        return new ApiResponse("Rollar yangilandi", true);
    }

    /** Hodimga menejer biriktirish (userlar sahifasidagi select'dan) */
    @Transactional
    public ApiResponse updateUserMenejer(Long userId, Long menejerId) {
        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ApiResponse("Hodim topilmadi", false);
        }

        if (menejerId == null) {
            user.setMenejer(null);
        } else {
            if (menejerId.equals(userId)) {
                return new ApiResponse("Hodim o'ziga o'zi menejer bo'la olmaydi", false);
            }
            Users menejer = usersRepository.findById(menejerId).orElse(null);
            if (menejer == null) {
                return new ApiResponse("Menejer topilmadi", false);
            }
            user.setMenejer(menejer);
        }
        usersRepository.save(user);

        tarixService.yoz("Hodim", "Menejer biriktirildi", user.getId(), kim(user),
                "Menejer: " + (user.getMenejer() == null ? "—" : kim(user.getMenejer())));
        return new ApiResponse("Menejer biriktirildi", true);
    }

    // ================= PROFIL =================

    @Transactional(readOnly = true)
    public UserDto getProfil(String username) {
        Users u = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User yo'q"));
        return toDto(u);
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

        tarixService.yoz("Profil", "Tahrirlandi", user.getId(), kim(user), null);
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

        tarixService.yoz("Profil", "Parol o'zgartirildi", user.getId(), kim(user), null);
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

        String nomi = kim(user);
        String username = user.getUsername();

        // Boshqa jadvallardagi FK izlarini bo'shatamiz — tarix/sotuv/o'tkazma
        // yozuvlarining o'zi qoladi (ular username/nom nusxasini alohida saqlaydi),
        // faqat "kim qildi" bog'lanishi bo'sh qoladi.
        user.getRollar().clear();
        for (Magazin m : magazinRepository.findByHodimlar_Id(id)) {
            m.getHodimlar().remove(user);
        }
        usersRepository.menejerlikniTozalash(id);
        mahsulotRepository.yaratganUserniTozalash(id);
        otkazmaRepository.yuborganniTozalash(id);
        otkazmaRepository.halQilganniTozalash(id);
        sotuvRepository.sotganniTozalash(id);
        sotuvRepository.qaytarganniTozalash(id);
        sotuvRepository.katmJavobBerganniTozalash(id);
        tarixRepository.userniTozalash(id);

        usersRepository.delete(user);

        tarixService.yoz("Hodim", "O'chirildi", id, nomi, "Username: " + username);
        return new ApiResponse("Hodim o'chirildi", true);
    }

    // ================= YORDAMCHI =================

    /** Tarixda ko'rinadigan nom: FISH bo'lsa u, bo'lmasa username */
    private String kim(Users u) {
        return u.getFish() != null && !u.getFish().isBlank() ? u.getFish() : u.getUsername();
    }

    private String rollarMatn(Users u) {
        if (u.getRollar() == null || u.getRollar().isEmpty()) return "—";
        return u.getRollar().stream().map(Rol::getNomi)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}