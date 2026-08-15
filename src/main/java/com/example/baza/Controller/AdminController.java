package com.example.baza.Controller;

import com.example.baza.Configurations.TokenGenerator;
import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.ArizaDto;
import com.example.baza.Dto.ArizaHolatiSaveDto;
import com.example.baza.Dto.ArizaSaveDto;
import com.example.baza.Dto.InstagramAkkauntDto;
import com.example.baza.Dto.InstagramAkkauntSaveDto;
import com.example.baza.Dto.KategoriyaDto;
import com.example.baza.Dto.ImportNatijaDto;
import com.example.baza.Dto.KategoriyaSaveDto;
import com.example.baza.Dto.LoginDto;
import com.example.baza.Dto.MagazinDto;
import com.example.baza.Dto.MagazinQisqaDto;
import com.example.baza.Dto.MagazinSaveDto;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.MahsulotImportNatijaDto;
import com.example.baza.Dto.MahsulotQidirDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Dto.OtkazmaDto;
import com.example.baza.Dto.OtkazmaJavobDto;
import com.example.baza.Dto.OtkazmaSaveDto;
import com.example.baza.Dto.ParolUpdateDto;
import com.example.baza.Dto.ProfilUpdateDto;
import com.example.baza.Dto.RolDto;
import com.example.baza.Dto.RolSaveDto;
import com.example.baza.Dto.RuxsatDto;
import com.example.baza.Dto.HodimQisqaDto;
import com.example.baza.Dto.HolatBelgilaDto;
import com.example.baza.Dto.KatmJavobDto;
import com.example.baza.Dto.KutilayotganMahsulotDto;
import com.example.baza.Dto.MagazinTanlashDto;
import com.example.baza.Dto.SerialKodDto;
import com.example.baza.Dto.SotuvDto;
import com.example.baza.Dto.SotuvJavobDto;
import com.example.baza.Dto.SotuvSaveDto;
import com.example.baza.Dto.StatistikaDto;
import com.example.baza.Dto.TarixSahifaDto;
import com.example.baza.Dto.UsdKursDto;
import com.example.baza.Dto.UserAddDto;
import com.example.baza.Dto.UserDto;
import com.example.baza.Entity.Users;
import com.example.baza.Service.ArizaService;
import com.example.baza.Service.InstagramAkkauntService;
import com.example.baza.Service.KategoriyaService;
import com.example.baza.Service.MagazinService;
import com.example.baza.Service.MahsulotImportService;
import com.example.baza.Service.MahsulotService;
import com.example.baza.Service.OtkazmaService;
import com.example.baza.Service.KutilayotganMahsulotService;
import com.example.baza.Service.RolService;
import com.example.baza.Service.SotuvService;
import com.example.baza.Service.StatistikaService;
import com.example.baza.Service.TarixService;
import com.example.baza.Service.ValyutaService;
import com.example.baza.Service.UserService;
import com.example.baza.Service.BarkodService;
import com.example.baza.Service.SozlamaService;
import com.example.baza.Dto.SozlamaDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final StatistikaService statistikaService;
    private final MahsulotImportService mahsulotImportService;
    private final ArizaService arizaService;
    private final InstagramAkkauntService instagramAkkauntService;
    private final BarkodService barkodService;
    private final SozlamaService sozlamaService;
    private final KutilayotganMahsulotService kutilayotganMahsulotService;

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
                           TarixService tarixService,
                           StatistikaService statistikaService,
                           MahsulotImportService mahsulotImportService,
                           ArizaService arizaService,
                           InstagramAkkauntService instagramAkkauntService,
                           BarkodService barkodService,
                           SozlamaService sozlamaService,
                           KutilayotganMahsulotService kutilayotganMahsulotService) {
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
        this.statistikaService = statistikaService;
        this.mahsulotImportService = mahsulotImportService;
        this.arizaService = arizaService;
        this.instagramAkkauntService = instagramAkkauntService;
        this.barkodService = barkodService;
        this.sozlamaService = sozlamaService;
        this.kutilayotganMahsulotService = kutilayotganMahsulotService;
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
    @PreAuthorize("hasAnyAuthority('HODIM_BOSHQARISH', 'MAGAZIN_BOSHQARISH', 'INSTAGRAM_AKKAUNT_BOSHQARISH')")
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

    /** Joriy hodim mas'ul bo'lgan magazinlar — "mahsulot so'rash" moda maydonini to'ldiradi */
    @GetMapping("/get-mening-magazinlarim")
    @ResponseBody
    public ResponseEntity<List<MagazinQisqaDto>> getMeningMagazinlarim(Authentication authentication) {
        return ResponseEntity.ok(magazinService.getMeningMagazinlarim(authentication.getName()));
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

    /**
     * Mahsulotlarni Excel (POS eksporti) fayldan ommaviy qo'shish.
     * Ustunlar: Товар, Категория, Остаток, Единица по умолчанию (Метр/Штук).
     */
    @PostMapping("/import-mahsulot")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<MahsulotImportNatijaDto> importMahsulot(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "magazinId", required = false) Long magazinId) {
        MahsulotImportNatijaDto res = mahsulotImportService.importQil(file, magazinId);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= KUTILAYOTGAN MAHSULOTLAR (zavod "Chiqim" Excel + skaner tasdiqlash) =================

    @GetMapping("/kutilayotgan-mahsulotlar")
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public String kutilayotganMahsulotlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "kutilayotgan-mahsulotlar");
        return "kutilayotgan-mahsulotlar";
    }

    /** Zavod "Chiqim" Excel fayli — mahsulotga darhol qo'shilmaydi, "kutilmoqda" holatida saqlanadi */
    @PostMapping("/kutilayotgan-mahsulot-yukla")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<MahsulotImportNatijaDto> kutilayotganMahsulotYukla(
            @RequestParam("file") MultipartFile file) {
        MahsulotImportNatijaDto res = kutilayotganMahsulotService.excelniYukla(file);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @GetMapping("/get-kutilayotgan-mahsulotlar")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<List<KutilayotganMahsulotDto>> getKutilayotganMahsulotlar() {
        return ResponseEntity.ok(kutilayotganMahsulotService.royxat());
    }

    /** Fizik mahsulot skanerlanganda (seriya raqami) — ro'yxatdagi mos qatorga ptichka qo'yadi */
    @PostMapping("/kutilayotgan-mahsulot-skaner")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> kutilayotganMahsulotSkaner(@RequestBody SerialKodDto dto) {
        ApiResponse res = kutilayotganMahsulotService.skanerBelgila(dto.serialKod());
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /** Ptichkani qo'lda belgilash/bekor qilish (skaner ishlamay qolsa) */
    @PostMapping("/kutilayotgan-mahsulot-belgila/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> kutilayotganMahsulotBelgila(@PathVariable Long id,
                                                                    @RequestBody HolatBelgilaDto dto) {
        ApiResponse res = kutilayotganMahsulotService.qolBelgila(id, Boolean.TRUE.equals(dto.holat()));
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/kutilayotgan-mahsulot/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> kutilayotganMahsulotOchirish(@PathVariable Long id) {
        ApiResponse res = kutilayotganMahsulotService.ochirish(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /** Belgilangan (skanerlangan) qatorlarni haqiqiy Mahsulotga aylantiradi, magazin shu yerda tanlanadi */
    @PostMapping("/kutilayotgan-mahsulotlar-tasdiqlash")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> kutilayotganMahsulotlarTasdiqlash(@RequestBody MagazinTanlashDto dto) {
        ApiResponse res = kutilayotganMahsulotService.tasdiqlaHammasi(dto.magazinId());
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /**
     * Mahsulot kamchiliklari — mavjud mahsulotlardan nomi, kodi, zavod narxi,
     * kategoriyasi, magazini yoki o'lchami yetishmayotganlari. Ro'yxat
     * "/admin/get-mahsulotlar"dan olib, brauzerda filtrlanadi (backend
     * o'zgarishi shart emas); shu sahifa faqat marshrut sifatida kerak.
     */
    @GetMapping("/mahsulot-kamchiliklari")
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public String mahsulotKamchiliklariPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "mahsulot-kamchiliklari");
        return "mahsulot-kamchiliklari";
    }

    /** Bitta mahsulotning to'liq ma'lumoti — rasm, QR kod, shtrix-kod shu yerda */
    @GetMapping("/mahsulot/{id}")
    public String mahsulotDetailPage(@PathVariable Long id, Authentication authentication, Model model) {
        if (mahsulotService.getMahsulotDto(id, authentication.getName()).isEmpty()) {
            return "redirect:/admin/mahsulotlar";
        }
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "mahsulot-detail");
        model.addAttribute("mahsulotId", id);
        return "mahsulot-detail";
    }

    @GetMapping("/get-mahsulot/{id}")
    @ResponseBody
    public ResponseEntity<MahsulotDto> getMahsulot(@PathVariable Long id, Authentication authentication) {
        return mahsulotService.getMahsulotDto(id, authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Shu mahsulotga tegishli barcha seriya raqamlari (bir nechta bo'lishi mumkin —
     * kodi/o'lchami bir xil bo'lgan dona qo'shilganda eskilari o'chmasdan to'planadi).
     */
    @GetMapping("/mahsulot/{id}/seriyalar")
    @ResponseBody
    public ResponseEntity<List<String>> mahsulotSeriyalari(@PathVariable Long id) {
        return ResponseEntity.ok(mahsulotService.seriyalarRoyxati(id));
    }

    /** Shtrix-kod skaneri o'qigan kod bo'yicha mahsulotni topadi — dashboarddagi skaner shu yerga so'rov yuboradi */
    @GetMapping("/mahsulot-qidir-kod/{kod}")
    @ResponseBody
    public ResponseEntity<MahsulotQidirDto> mahsulotQidirKod(@PathVariable String kod, Authentication authentication) {
        return mahsulotService.kodBoyichaQidir(kod, authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/mahsulot/{id}/rasm")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> mahsulotRasmYuklash(@PathVariable Long id,
                                                            @RequestParam("file") MultipartFile file) {
        ApiResponse res = mahsulotService.rasmniYuklash(id, file);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/mahsulot/{id}/rasm")
    @ResponseBody
    @PreAuthorize("hasAuthority('MAHSULOT_QOSHISH')")
    public ResponseEntity<ApiResponse> mahsulotRasmOchirish(@PathVariable Long id) {
        ApiResponse res = mahsulotService.rasmniOchirish(id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /** QR kod — skaner qilinsa shu mahsulotning detail sahifasiga olib boradi */
    @GetMapping(value = "/mahsulot/{id}/qr-kod", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> mahsulotQrKod(@PathVariable Long id, HttpServletRequest request,
                                                 Authentication authentication) {
        if (mahsulotService.getMahsulotDto(id, authentication.getName()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String port = (request.getServerPort() == 80 || request.getServerPort() == 443)
                ? "" : ":" + request.getServerPort();
        String url = request.getScheme() + "://" + request.getServerName() + port + "/admin/mahsulot/" + id;
        try {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                    .body(barkodService.qrKod(url, 300));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Shtrix-kod (Code128) — mahsulot kodini kodlaydi */
    @GetMapping(value = "/mahsulot/{id}/shtrix-kod", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> mahsulotShtrixKod(@PathVariable Long id, Authentication authentication) {
        MahsulotDto mahsulot = mahsulotService.getMahsulotDto(id, authentication.getName()).orElse(null);
        if (mahsulot == null || mahsulot.kod() == null || mahsulot.kod().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                    .body(barkodService.shtrixKod(mahsulot.kod(), 300, 100));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

    /** "Jo'natish"ning teskarisi — boshqa magazindagi mahsulotni o'z magazinimga so'rayman */
    @PostMapping("/otkazma-sorash")
    @ResponseBody
    @PreAuthorize("hasAuthority('OTKAZMA_YUBORISH')")
    public ResponseEntity<ApiResponse> otkazmaSorash(Authentication authentication,
                                                      @RequestBody OtkazmaSaveDto dto) {
        ApiResponse res = otkazmaService.sorash(authentication.getName(), dto);
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

    /** Sotish modalidagi "Sotuvchi" tanlovi - shu magazinga mas'ul hodimlar ro'yxati */
    @GetMapping("/get-magazin-hodimlari/{magazinId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('SOTUV_QILISH')")
    public ResponseEntity<List<HodimQisqaDto>> getMagazinHodimlari(Authentication authentication,
                                                                    @PathVariable Long magazinId) {
        return ResponseEntity.ok(sotuvService.magazinHodimlari(authentication.getName(), magazinId));
    }

    /** Mahsulotni KATMga o'tkazish - mahsulot band bo'ladi, javob kutiladi */
    @PostMapping("/katmga-otkazish")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATM_OTKAZISH')")
    public ResponseEntity<ApiResponse> katmgaOtkazish(Authentication authentication,
                                                      @RequestBody SotuvSaveDto dto) {
        ApiResponse res = sotuvService.katmgaOtkazish(authentication.getName(), dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /** Gilamga yuborilmay qolgan KATM so'rovini qayta yuborish */
    @PostMapping("/katm-qayta-yuborish/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATM_OTKAZISH')")
    public ResponseEntity<ApiResponse> katmQaytaYuborish(Authentication authentication,
                                                         @PathVariable Long id) {
        ApiResponse res = sotuvService.qaytaYuborish(authentication.getName(), id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /**
     * KATM javobi. Odatda javob GILAM dasturidan keladi (POST /api/katm/javob),
     * bu endpoint esa qo'lda bekor qilish uchun: tasdiq=false -> mahsulot qaytadi.
     */
    @PostMapping("/katm-javob/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('KATM_JAVOB')")
    public ResponseEntity<ApiResponse> katmJavob(Authentication authentication,
                                                 @PathVariable Long id,
                                                 @RequestBody KatmJavobDto dto) {
        ApiResponse res = sotuvService.katmJavobi(authentication.getName(), id,
                dto != null && Boolean.TRUE.equals(dto.tasdiq()),
                dto == null ? null : dto.izoh());
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

    // ================= KASSA =================
    @GetMapping("/kassa")
    @PreAuthorize("hasAuthority('KASSA_KORISH')")
    public String kassaPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "kassa");
        return "kassa";
    }

    /** Barcha magazinlardagi sotuvlar (tushum + KATM) — magazin mansubligidan qat'i nazar */
    @GetMapping("/get-kassa")
    @ResponseBody
    @PreAuthorize("hasAuthority('KASSA_KORISH')")
    public ResponseEntity<List<SotuvDto>> getKassa(
            @RequestParam(name = "sanadan", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanadan,
            @RequestParam(name = "sanagacha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanagacha) {
        return ResponseEntity.ok(sotuvService.getKassa(sanadan, sanagacha));
    }

    /** Owner (yoki KASSA_TASDIQLASH huquqli hodim) sotuv tushumini kassada qabul qiladi */
    @PostMapping("/kassa-qabul/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('KASSA_TASDIQLASH')")
    public ResponseEntity<ApiResponse> kassaQabul(Authentication authentication, @PathVariable Long id) {
        ApiResponse res = sotuvService.kassaQabulQildim(authentication.getName(), id);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= SOZLAMALAR =================
    @GetMapping("/sozlamalar")
    @PreAuthorize("hasAuthority('SOZLAMA_BOSHQARISH')")
    public String sozlamalarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "sozlamalar");
        return "sozlamalar";
    }

    /**
     * Sozlamalarni o'qish — istalgan tizimga kirgan foydalanuvchiga ochiq (Ruxsat
     * bilan cheklanmagan), chunki "chop etish" har qanday sahifadan chaqirilishi
     * mumkin va shu sozlamadan foydalanadi. Faqat O'ZGARTIRISH cheklangan.
     */
    @GetMapping("/get-sozlamalar")
    @ResponseBody
    public ResponseEntity<SozlamaDto> getSozlamalar() {
        return ResponseEntity.ok(sozlamaService.olish());
    }

    @PostMapping("/sozlamalar-yangilash")
    @ResponseBody
    @PreAuthorize("hasAuthority('SOZLAMA_BOSHQARISH')")
    public ResponseEntity<ApiResponse> sozlamalarYangilash(@RequestBody SozlamaDto dto) {
        ApiResponse res = sozlamaService.yangilash(dto);
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

    // ================= STATISTIKA =================
    @GetMapping("/statistika")
    @PreAuthorize("hasAuthority('STATISTIKA_KORISH')")
    public String statistikaPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "statistika");
        return "statistika";
    }

    @GetMapping("/get-statistika")
    @ResponseBody
    @PreAuthorize("hasAuthority('STATISTIKA_KORISH')")
    public ResponseEntity<StatistikaDto> getStatistika(Authentication authentication) {
        return ResponseEntity.ok(statistikaService.hisobla(authentication.getName()));
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

    // ================= ARIZALAR (Instagram) =================
    @GetMapping("/arizalar")
    @PreAuthorize("hasAuthority('ARIZA_KORISH')")
    public String arizalarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "arizalar");
        return "arizalar";
    }

    @GetMapping("/get-arizalar")
    @ResponseBody
    @PreAuthorize("hasAuthority('ARIZA_KORISH')")
    public ResponseEntity<List<ArizaDto>> getArizalar(Authentication authentication) {
        return ResponseEntity.ok(arizaService.getAllArizalar(authentication.getName()));
    }

    @PostMapping("/add-ariza")
    @ResponseBody
    @PreAuthorize("hasAuthority('ARIZA_BOSHQARISH')")
    public ResponseEntity<ApiResponse> addAriza(@RequestBody ArizaSaveDto dto, Authentication authentication) {
        Users joriy = (Users) authentication.getPrincipal();
        ApiResponse res = arizaService.qoshish(dto, joriy);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-ariza-holati/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ARIZA_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateArizaHolati(@PathVariable Long id,
                                                         @RequestBody ArizaHolatiSaveDto dto,
                                                         Authentication authentication) {
        Users joriy = (Users) authentication.getPrincipal();
        ApiResponse res = arizaService.holatniYangilash(id, dto, joriy);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    // ================= INSTAGRAM AKKAUNTLAR =================
    @GetMapping("/instagram-akkauntlar")
    @PreAuthorize("hasAuthority('INSTAGRAM_AKKAUNT_BOSHQARISH')")
    public String instagramAkkauntlarPage(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("page", "instagram-akkauntlar");
        return "instagram-akkauntlar";
    }

    @GetMapping("/get-instagram-akkauntlar")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('INSTAGRAM_AKKAUNT_BOSHQARISH', 'ARIZA_BOSHQARISH')")
    public ResponseEntity<List<InstagramAkkauntDto>> getInstagramAkkauntlar(Authentication authentication) {
        boolean boshqarishHuquqi = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("INSTAGRAM_AKKAUNT_BOSHQARISH"));
        List<InstagramAkkauntDto> natija = boshqarishHuquqi
                ? instagramAkkauntService.getAllAkkauntlar()
                : instagramAkkauntService.getMeningAkkauntlarim(authentication.getName());
        return ResponseEntity.ok(natija);
    }

    @PostMapping("/add-instagram-akkaunt")
    @ResponseBody
    @PreAuthorize("hasAuthority('INSTAGRAM_AKKAUNT_BOSHQARISH')")
    public ResponseEntity<ApiResponse> addInstagramAkkaunt(@RequestBody InstagramAkkauntSaveDto dto) {
        ApiResponse res = instagramAkkauntService.addAkkaunt(dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @PutMapping("/update-instagram-akkaunt/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('INSTAGRAM_AKKAUNT_BOSHQARISH')")
    public ResponseEntity<ApiResponse> updateInstagramAkkaunt(@PathVariable Long id,
                                                              @RequestBody InstagramAkkauntSaveDto dto) {
        ApiResponse res = instagramAkkauntService.updateAkkaunt(id, dto);
        return res.holat() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/delete-instagram-akkaunt/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('INSTAGRAM_AKKAUNT_BOSHQARISH')")
    public ResponseEntity<ApiResponse> deleteInstagramAkkaunt(@PathVariable Long id) {
        ApiResponse res = instagramAkkauntService.deleteAkkaunt(id);
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