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
import com.example.baza.Dto.UserAddDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Service.KategoriyaService;
import com.example.baza.Service.MagazinService;
import com.example.baza.Service.MahsulotService;
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

    public AdminController(AuthenticationManager authenticationManager,
                           TokenGenerator tokenGenerator,
                           UserService userService,
                           MagazinService magazinService,
                           MahsulotService mahsulotService,
                           KategoriyaService kategoriyaService) {
        this.authenticationManager = authenticationManager;
        this.tokenGenerator = tokenGenerator;
        this.userService = userService;
        this.magazinService = magazinService;
        this.mahsulotService = mahsulotService;
        this.kategoriyaService = kategoriyaService;
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
    @PreAuthorize("hasRole('owner')")
    public String usersPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "users");
        return "users";
    }

    @GetMapping("/get-users")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/add-user")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<ApiResponse> addUser(@RequestBody UserAddDto dto) {
        ApiResponse res = userService.addUser(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-user/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
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
    @PreAuthorize("hasRole('owner')")
    public String magazinlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "magazinlar");
        return "magazinlar";
    }

    @GetMapping("/get-magazinlar")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<List<MagazinDto>> getMagazinlar() {
        return ResponseEntity.ok(magazinService.getAllMagazinlar());
    }

    @PostMapping("/add-magazin")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<ApiResponse> addMagazin(@RequestBody MagazinSaveDto dto) {
        ApiResponse res = magazinService.addMagazin(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-magazin/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<ApiResponse> updateMagazin(@PathVariable Long id,
                                                     @RequestBody MagazinSaveDto dto) {
        ApiResponse res = magazinService.updateMagazin(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-magazin/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('owner')")
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
    public ResponseEntity<ApiResponse> addMahsulot(@RequestBody MahsulotSaveDto dto) {
        ApiResponse res = mahsulotService.addMahsulot(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-mahsulot/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> updateMahsulot(@PathVariable Long id,
                                                      @RequestBody MahsulotSaveDto dto) {
        ApiResponse res = mahsulotService.updateMahsulot(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-mahsulot/{id}")
    @ResponseBody
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
    public ResponseEntity<ApiResponse> addKategoriya(@RequestBody KategoriyaSaveDto dto) {
        ApiResponse res = kategoriyaService.addKategoriya(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-kategoriya/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> updateKategoriya(@PathVariable Long id,
                                                        @RequestBody KategoriyaSaveDto dto) {
        ApiResponse res = kategoriyaService.updateKategoriya(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-kategoriya/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> deleteKategoriya(@PathVariable Long id) {
        ApiResponse res = kategoriyaService.deleteKategoriya(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= LOGOUT =================
    @GetMapping("/logout")
    public void logout(HttpServletResponse response) throws IOException {
        Cookie cookie = new Cookie("Auth", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // cookie o'chiriladi
        response.addCookie(cookie);
        response.sendRedirect("/");
    }
}
