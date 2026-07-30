package com.example.baza.Controller;

import com.example.baza.Configurations.TokenGenerator;
import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KategoriyaDto;
import com.example.baza.Dto.KategoriyaSaveDto;
import com.example.baza.Dto.LoginDto;
import com.example.baza.Dto.MagazinDto;
import com.example.baza.Dto.MagazinQisqaDto;
import com.example.baza.Dto.MagazinSaveDto;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Dto.ParolUpdateDto;
import com.example.baza.Dto.ProfilUpdateDto;
import com.example.baza.Dto.RolDto;
import com.example.baza.Dto.RolSaveDto;
import com.example.baza.Dto.RuxsatDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Dto.UserAddDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Service.KategoriyaService;
import com.example.baza.Service.MagazinService;
import com.example.baza.Service.MahsulotService;
import com.example.baza.Service.RolService;
import com.example.baza.Service.ValyutaService;
import com.example.baza.Service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int TOKEN_MUDDATI_SEKUND = 60 * 60 * 24; // 24 soat (TokenGenerator bilan bir xil)

    private final AuthenticationManager authenticationManager;
    private final TokenGenerator tokenGenerator;
    private final UserService userService;
    private final MagazinService magazinService;
    private final MahsulotService mahsulotService;
    private final KategoriyaService kategoriyaService;
    private final ValyutaService valyutaService;
    private final RolService rolService;

    public AdminController(AuthenticationManager authenticationManager,
                           TokenGenerator tokenGenerator,
                           UserService userService,
                           MagazinService magazinService,
                           MahsulotService mahsulotService,
                           KategoriyaService kategoriyaService,
                           ValyutaService valyutaService,
                           RolService rolService) {
        this.authenticationManager = authenticationManager;
        this.tokenGenerator = tokenGenerator;
        this.userService = userService;
        this.magazinService = magazinService;
        this.mahsulotService = mahsulotService;
        this.kategoriyaService = kategoriyaService;
        this.valyutaService = valyutaService;
        this.rolService = rolService;
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<ApiResponse> login(@RequestBody LoginDto loginDto,
                                             HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.username(), loginDto.password()));

            String token = tokenGenerator.generateToken(auth.getName());

            Cookie cookie = new Cookie("Auth", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(TOKEN_MUDDATI_SEKUND);
            response.addCookie(cookie);

            return ResponseEntity.ok(new ApiResponse("Muvaffaqiyatli kirildi", true));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Xato username yoki parol!", false));
        }
    }

    // ================= DASHBOARD =================
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "dashboard");
        return "dashboard";
    }

    // ================= HODIMLAR (USERS) =================
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('HODIM_BOSHQARISH')")
    public String usersPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "users");
        return "users";
    }

    @GetMapping("/get-users")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('HODIM_BOSHQARISH', 'MAGAZIN_BOSHQARISH')")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/add-user")
    @ResponseBody
    @PreAuthorize("hasAuthority('HODIM_BOSHQARISH')")
    public ResponseEntity<ApiResponse> addUser(@RequestBody UserAddDto dto) {
        ApiResponse res = userService.addUser(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-user/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('HODIM_BOSHQARISH')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        ApiResponse res = userService.deleteUser(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= PROFIL =================
    @GetMapping("/profil")
    public String profilPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "profil");
        return "profil";
    }

    @GetMapping("/get-profil")
    @ResponseBody
    public ResponseEntity<UserDto> getProfil(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfil(authentication.getName()));
    }

    @PutMapping("/update-profil")
    @ResponseBody
    public ResponseEntity<ApiResponse> updateProfil(@RequestBody ProfilUpdateDto dto,
                                                    Authentication authentication) {
        ApiResponse res = userService.updateProfil(authentication.getName(), dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-parol")
    @ResponseBody
    public ResponseEntity<ApiResponse> updateParol(@RequestBody ParolUpdateDto dto,
                                                   Authentication authentication) {
        ApiResponse res = userService.updateParol(authentication.getName(), dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= MAGAZINLAR =================
    @GetMapping("/magazinlar")
    @PreAuthorize("hasAuthority('MAGAZIN_BOSHQARISH')")
    public String magazinlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "magazinlar");
        return "magazinlar";
    }

    @GetMapping("/get-magazinlar")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAGAZIN_BOSHQARISH')")
    public ResponseEntity<List<MagazinDto>> getMagazinlar() {
        return ResponseEntity.ok(magazinService.getAllMagazinlar());
    }

    @PostMapping("/add-magazin")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAGAZIN_BOSHQARISH')")
    public ResponseEntity<ApiResponse> addMagazin(@RequestBody MagazinSaveDto dto) {
        ApiResponse res = magazinService.addMagazin(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-magazin/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAGAZIN_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateMagazin(@PathVariable Long id,
                                                     @RequestBody MagazinSaveDto dto) {
        ApiResponse res = magazinService.updateMagazin(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-magazin/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAGAZIN_BOSHQARISH')")
    public ResponseEntity<ApiResponse> deleteMagazin(@PathVariable Long id) {
        ApiResponse res = magazinService.deleteMagazin(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= MAHSULOTLAR =================
    @GetMapping("/mahsulotlar")
    public String mahsulotlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "mahsulotlar");
        return "mahsulotlar";
    }

    @GetMapping("/get-mahsulotlar")
    @ResponseBody
    public ResponseEntity<List<MahsulotDto>> getMahsulotlar() {
        return ResponseEntity.ok(mahsulotService.getAllMahsulotlar());
    }

    @GetMapping("/get-magazin-nomlar")
    @ResponseBody
    public ResponseEntity<List<MagazinQisqaDto>> getMagazinNomlar() {
        return ResponseEntity.ok(magazinService.getMagazinNomlar());
    }

    @PostMapping("/add-mahsulot")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> addMahsulot(@RequestBody MahsulotSaveDto dto) {
        ApiResponse res = mahsulotService.addMahsulot(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-mahsulot/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> updateMahsulot(@PathVariable Long id,
                                                      @RequestBody MahsulotSaveDto dto) {
        ApiResponse res = mahsulotService.updateMahsulot(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-mahsulot/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_OCHIRISH')")
    public ResponseEntity<ApiResponse> deleteMahsulot(@PathVariable Long id) {
        ApiResponse res = mahsulotService.deleteMahsulot(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= KATEGORIYALAR =================
    @GetMapping("/kategoriyalar")
    public String kategoriyalarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "kategoriyalar");
        return "kategoriyalar";
    }

    @GetMapping("/get-kategoriyalar")
    @ResponseBody
    public ResponseEntity<List<KategoriyaDto>> getKategoriyalar() {
        return ResponseEntity.ok(kategoriyaService.getAllKategoriyalar());
    }

    @PostMapping("/add-kategoriya")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATEGORIYA_BOSHQARISH')")
    public ResponseEntity<ApiResponse> addKategoriya(@RequestBody KategoriyaSaveDto dto) {
        ApiResponse res = kategoriyaService.addKategoriya(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-kategoriya/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATEGORIYA_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateKategoriya(@PathVariable Long id,
                                                        @RequestBody KategoriyaSaveDto dto) {
        ApiResponse res = kategoriyaService.updateKategoriya(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-kategoriya/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATEGORIYA_BOSHQARISH')")
    public ResponseEntity<ApiResponse> deleteKategoriya(@PathVariable Long id) {
        ApiResponse res = kategoriyaService.deleteKategoriya(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-user-rollar/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('HODIM_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateUserRollar(@PathVariable Long id,
                                                        @RequestBody List<Long> rolIds) {
        ApiResponse res = userService.updateUserRollar(id, rolIds);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-user-menejer/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('HODIM_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateUserMenejer(@PathVariable Long id,
                                                         @RequestParam(required = false) Long menejerId) {
        ApiResponse res = userService.updateUserMenejer(id, menejerId);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= ROLLAR =================
    @GetMapping("/rollar")
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public String rollarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "rollar");
        return "rollar";
    }

    @GetMapping("/get-rollar")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROL_BOSHQARISH', 'HODIM_BOSHQARISH')")
    public ResponseEntity<List<RolDto>> getRollar() {
        return ResponseEntity.ok(rolService.getAllRollar());
    }

    @PostMapping("/add-rol")
    @ResponseBody
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public ResponseEntity<ApiResponse> addRol(@RequestBody RolSaveDto dto) {
        ApiResponse res = rolService.addRol(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-rol/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateRol(@PathVariable Long id,
                                                 @RequestBody RolSaveDto dto) {
        ApiResponse res = rolService.updateRol(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-rol/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public ResponseEntity<ApiResponse> deleteRol(@PathVariable Long id) {
        ApiResponse res = rolService.deleteRol(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= RUXSATLAR =================
    @GetMapping("/ruxsatlar")
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public String ruxsatlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "ruxsatlar");
        return "ruxsatlar";
    }

    @GetMapping("/get-ruxsat-turlari")
    @ResponseBody
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public ResponseEntity<List<RuxsatDto>> getRuxsatTurlari() {
        return ResponseEntity.ok(rolService.getRuxsatTurlari());
    }

    @PutMapping("/update-rol-ruxsatlar/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ROL_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateRolRuxsatlar(@PathVariable Long id,
                                                          @RequestBody List<String> kodlar) {
        ApiResponse res = rolService.updateRuxsatlar(id, kodlar);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= VALYUTA KURSI =================
    @GetMapping("/get-usd-kurs")
    @ResponseBody
    public ResponseEntity<UsdKursDto> getUsdKurs() {
        return ResponseEntity.ok(valyutaService.getUsdKurs());
    }

    // ================= LOGOUT =================
    @GetMapping("/logout")
    public void logout(HttpServletResponse response) throws IOException {
        Cookie cookie = new Cookie("Auth", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // cookie o'chiriladi
        response.addCookie(cookie);
        response.sendRedirect("/?xabar="
                + URLEncoder.encode("Tizimdan chiqdingiz", StandardCharsets.UTF_8)
                + "&tur=info");
    }
}
