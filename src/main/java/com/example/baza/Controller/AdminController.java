package com.example.baza.Controller;

import com.example.baza.Configurations.TokenGenerator;
import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KategoriyaDto;
import com.example.baza.Dto.ImportNatijaDto;
import com.example.baza.Dto.KategoriyaSaveDto;
import com.example.baza.Dto.LoginDto;
import com.example.baza.Dto.MagazinDto;
import com.example.baza.Dto.MagazinQisqaDto;
import com.example.baza.Dto.MagazinSaveDto;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Dto.OtkazmaDto;
import com.example.baza.Dto.OtkazmaJavobDto;
import com.example.baza.Dto.OtkazmaSaveDto;
import com.example.baza.Dto.ParolUpdateDto;
import com.example.baza.Dto.ProfilUpdateDto;
import com.example.baza.Dto.RolDto;
import com.example.baza.Dto.RolSaveDto;
import com.example.baza.Dto.RuxsatDto;
import com.example.baza.Dto.SotuvDto;
import com.example.baza.Dto.SotuvJavobDto;
import com.example.baza.Dto.SotuvSaveDto;
import com.example.baza.Dto.TarixSahifaDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Dto.UserAddDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Service.KategoriyaService;
import com.example.baza.Service.MagazinService;
import com.example.baza.Service.MahsulotService;
import com.example.baza.Service.OtkazmaService;
import com.example.baza.Service.RolService;
import com.example.baza.Service.SotuvService;
import com.example.baza.Service.TarixService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    private final OtkazmaService otkazmaService;
    private final SotuvService sotuvService;
    private final TarixService tarixService;

    public AdminController(AuthenticationManager authenticationManager,
                           TokenGenerator tokenGenerator,
                           UserService userService,
                           MagazinService magazinService,
                           MahsulotService mahsulotService,
                           KategoriyaService kategoriyaService,
                           ValyutaService valyutaService,
                           RolService rolService,
                           OtkazmaService otkazmaService,
                           SotuvService sotuvService,
                           TarixService tarixService) {
        this.authenticationManager = authenticationManager;
        this.tokenGenerator = tokenGenerator;
        this.userService = userService;
        this.magazinService = magazinService;
        this.mahsulotService = mahsulotService;
        this.kategoriyaService = kategoriyaService;
        this.valyutaService = valyutaService;
        this.rolService = rolService;
        this.otkazmaService = otkazmaService;
        this.sotuvService = sotuvService;
        this.tarixService = tarixService;
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

            tarixService.yozUser(auth.getName(), "Kirish", "Tizimga kirdi",
                    null, auth.getName(), null);
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

    // ---- Mening mahsulotlarim (har bir hodim o'ziga tegishlisini ko'radi) ----
    @GetMapping("/mening-mahsulotlarim")
    public String meningMahsulotlarimPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "mening-mahsulotlarim");
        return "mening-mahsulotlarim";
    }

    @GetMapping("/get-mening-mahsulotlarim")
    @ResponseBody
    public ResponseEntity<List<MahsulotDto>> getMeningMahsulotlarim(Authentication authentication) {
        return ResponseEntity.ok(
                mahsulotService.getMeningMahsulotlarim(authentication.getName()));
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

    // ================= O'TKAZMALAR =================
    @GetMapping("/otkazmalar")
    public String otkazmalarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "otkazmalar");
        return "otkazmalar";
    }

    /** Menga kelayotgan (tasdiq kutayotgan) o'tkazmalar */
    @GetMapping("/get-kelayotgan-otkazmalar")
    @ResponseBody
    public ResponseEntity<List<OtkazmaDto>> getKelayotganOtkazmalar(Authentication authentication) {
        return ResponseEntity.ok(otkazmaService.kelayotganlar(authentication.getName()));
    }

    /** Men jo'natgan o'tkazmalar (barcha holat) */
    @GetMapping("/get-yuborgan-otkazmalar")
    @ResponseBody
    public ResponseEntity<List<OtkazmaDto>> getYuborganOtkazmalar(Authentication authentication) {
        return ResponseEntity.ok(otkazmaService.yuborganlarim(authentication.getName()));
    }

    /** Bitta mahsulotning o'tkazmalar tarixi */
    @GetMapping("/get-mahsulot-otkazmalari/{mahsulotId}")
    @ResponseBody
    public ResponseEntity<List<OtkazmaDto>> getMahsulotOtkazmalari(@PathVariable Long mahsulotId) {
        return ResponseEntity.ok(otkazmaService.mahsulotBoyicha(mahsulotId));
    }

    @PostMapping("/otkazma-yuborish")
    @ResponseBody
    @PreAuthorize("hasAuthority('OTKAZMA_YUBORISH')")
    public ResponseEntity<ApiResponse> otkazmaYuborish(Authentication authentication,
                                                       @RequestBody OtkazmaSaveDto dto) {
        ApiResponse res = otkazmaService.yuborish(authentication.getName(), dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PostMapping("/otkazma-qabul/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('OTKAZMA_QABUL')")
    public ResponseEntity<ApiResponse> otkazmaQabul(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @RequestBody(required = false) OtkazmaJavobDto dto) {
        ApiResponse res = otkazmaService.qabulQilish(authentication.getName(), id,
                dto == null ? null : dto.izoh());
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PostMapping("/otkazma-rad/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('OTKAZMA_QABUL')")
    public ResponseEntity<ApiResponse> otkazmaRad(Authentication authentication,
                                                  @PathVariable Long id,
                                                  @RequestBody(required = false) OtkazmaJavobDto dto) {
        ApiResponse res = otkazmaService.radEtish(authentication.getName(), id,
                dto == null ? null : dto.izoh());
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PostMapping("/otkazma-bekor/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('OTKAZMA_YUBORISH')")
    public ResponseEntity<ApiResponse> otkazmaBekor(Authentication authentication,
                                                    @PathVariable Long id) {
        ApiResponse res = otkazmaService.bekorQilish(authentication.getName(), id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= SOTUV =================
    @GetMapping("/sotuvlar")
    @PreAuthorize("hasAuthority('SOTUV_KORISH')")
    public String sotuvlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "sotuvlar");
        return "sotuvlar";
    }

    /** Hodim uchun faqat o'z magazin(lar)i sotuvlari, Owner uchun barchasi */
    @GetMapping("/get-sotuvlar")
    @ResponseBody
    @PreAuthorize("hasAuthority('SOTUV_KORISH')")
    public ResponseEntity<List<SotuvDto>> getSotuvlar(
            Authentication authentication,
            @RequestParam(name = "sanadan", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanadan,
            @RequestParam(name = "sanagacha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanagacha) {
        return ResponseEntity.ok(
                sotuvService.getSotuvlar(authentication.getName(), sanadan, sanagacha));
    }

    /** Bitta mahsulotning sotuv tarixi */
    @GetMapping("/get-mahsulot-sotuvlari/{mahsulotId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('SOTUV_KORISH')")
    public ResponseEntity<List<SotuvDto>> getMahsulotSotuvlari(@PathVariable Long mahsulotId) {
        return ResponseEntity.ok(sotuvService.mahsulotBoyicha(mahsulotId));
    }

    @PostMapping("/sotish")
    @ResponseBody
    @PreAuthorize("hasAuthority('SOTUV_QILISH')")
    public ResponseEntity<ApiResponse> sotish(Authentication authentication,
                                              @RequestBody SotuvSaveDto dto) {
        ApiResponse res = sotuvService.sotish(authentication.getName(), dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PostMapping("/sotuv-qaytarish/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('SOTUV_QAYTARISH')")
    public ResponseEntity<ApiResponse> sotuvQaytarish(Authentication authentication,
                                                      @PathVariable Long id,
                                                      @RequestBody(required = false) SotuvJavobDto dto) {
        ApiResponse res = sotuvService.qaytarish(authentication.getName(), id,
                dto == null ? null : dto.sabab());
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= TARIX =================
    @GetMapping("/tarix")
    @PreAuthorize("hasAuthority('TARIX_KORISH')")
    public String tarixPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "tarix");
        return "tarix";
    }

    @GetMapping("/get-tarix")
    @ResponseBody
    @PreAuthorize("hasAuthority('TARIX_KORISH')")
    public ResponseEntity<TarixSahifaDto> getTarix(
            @RequestParam(name = "bolim", defaultValue = "") String bolim,
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "sanadan", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanadan,
            @RequestParam(name = "sanagacha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanagacha,
            @RequestParam(name = "sahifa", defaultValue = "0") int sahifa) {
        return ResponseEntity.ok(
                tarixService.qidir(bolim, userId, q, sanadan, sanagacha, sahifa));
    }

    @GetMapping("/get-tarix-bolimlar")
    @ResponseBody
    @PreAuthorize("hasAuthority('TARIX_KORISH')")
    public ResponseEntity<List<String>> getTarixBolimlar() {
        return ResponseEntity.ok(tarixService.getBolimlar());
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

    /**
     * Kategoriyalarni Excel (.xlsx/.xls) yoki CSV fayldan ommaviy qo'shish.
     * Faylda faqat nomlar bo'ladi.
     */
    @PostMapping("/import-kategoriya")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATEGORIYA_BOSHQARISH')")
    public ResponseEntity<ImportNatijaDto> importKategoriya(
            @RequestParam("file") MultipartFile file) {
        ImportNatijaDto res = kategoriyaService.importQil(file);
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