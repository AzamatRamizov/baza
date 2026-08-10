package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.ArizaDto;
import com.example.baza.Dto.ArizaHolatiSaveDto;
import com.example.baza.Dto.ArizaSaveDto;
import com.example.baza.Entity.Ariza;
import com.example.baza.Entity.InstagramAkkaunt;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.ArizaRepository;
import com.example.baza.Repository.InstagramAkkauntRepository;
import com.example.baza.Repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArizaService {

    private static final Logger log = LoggerFactory.getLogger(ArizaService.class);

    private final ArizaRepository arizaRepository;
    private final InstagramAkkauntRepository instagramAkkauntRepository;
    private final UsersRepository usersRepository;
    private final InstagramGraphService instagramGraphService;
    private final TarixService tarixService;

    public ArizaService(ArizaRepository arizaRepository,
                        InstagramAkkauntRepository instagramAkkauntRepository,
                        UsersRepository usersRepository,
                        InstagramGraphService instagramGraphService,
                        TarixService tarixService) {
        this.arizaRepository = arizaRepository;
        this.instagramAkkauntRepository = instagramAkkauntRepository;
        this.usersRepository = usersRepository;
        this.instagramGraphService = instagramGraphService;
        this.tarixService = tarixService;
    }

    /**
     * Owner (tizim roli) — barcha akkauntlardan kelgan arizalarni ko'radi.
     * Boshqa xodim — faqat o'zi mas'ul bo'lgan Instagram akkaunt(lar)dan
     * kelgan arizalarni ko'radi (StatistikaService/MahsulotService'dagi
     * owner/hodim scoping pattern'i bilan bir xil).
     */
    @Transactional(readOnly = true)
    public List<ArizaDto> getAllArizalar(String username) {
        Users u = usersRepository.findByUsername(username).orElse(null);
        boolean owner = u != null && u.getRollar() != null
                && u.getRollar().stream().anyMatch(Rol::isTizimRoli);

        List<Ariza> arizalar = owner
                ? arizaRepository.findAllByOrderByCreatedTimeDesc()
                : arizaRepository.findAllByInstagramAkkaunt_MasulUser_UsernameOrderByCreatedTimeDesc(username);

        return arizalar.stream().map(this::dto).toList();
    }

    /**
     * Meta webhook'dan kelgan leadgen_id asosida to'liq ariza ma'lumotini
     * Graph API'dan olib saqlaydi. Bir xil leadgen_id ikkinchi marta kelsa
     * (Meta ba'zan qayta yuboradi) — o'tkazib yuboriladi. pageId orqali
     * qaysi InstagramAkkaunt'dan kelganini aniqlab, o'sha akkauntning
     * o'z Page Access Token'i bilan Graph API'ga so'rov yuboradi.
     */
    @Transactional
    public void webhookdanSaqlash(String pageId, String leadgenId, String formId, String adId) {
        if (leadgenId == null || leadgenId.isBlank()) return;

        if (arizaRepository.findByLeadgenId(leadgenId).isPresent()) {
            log.info("Ariza allaqachon saqlangan, o'tkazib yuborildi: leadgen_id={}", leadgenId);
            return;
        }

        InstagramAkkaunt akkaunt = pageId == null ? null
                : instagramAkkauntRepository.findByPageId(pageId).orElse(null);
        if (akkaunt == null) {
            log.warn("Bazada ro'yxatdan o'tmagan Page'dan lead keldi: page_id={} leadgen_id={} — " +
                    "Instagram akkauntlar bo'limiga shu Page'ni qo'shing", pageId, leadgenId);
        }

        InstagramGraphService.LeadMalumot malumot = akkaunt == null ? null
                : instagramGraphService.leadniOlish(leadgenId, akkaunt.getPageAccessToken());

        Ariza ariza = new Ariza();
        ariza.setLeadgenId(leadgenId);
        ariza.setInstagramAkkaunt(akkaunt);
        ariza.setFormId(malumot != null && malumot.formId() != null ? malumot.formId() : formId);
        ariza.setAdId(malumot != null && malumot.adId() != null ? malumot.adId() : adId);
        if (malumot != null) {
            ariza.setIsmFamiliya(malumot.ismFamiliya());
            ariza.setTelefon(malumot.telefon());
            ariza.setXomMalumot(malumot.xomMalumot());
        }
        arizaRepository.save(ariza);

        tarixService.yoz("Ariza", "Instagramdan keldi", ariza.getId(),
                ariza.getTelefon() != null ? ariza.getTelefon() : "leadgen_id: " + leadgenId, null);
    }

    /**
     * Qo'lda (Instagramdan tashqari, masalan telefon orqali kelgan) ariza qo'shish.
     * INSTAGRAM_AKKAUNT_BOSHQARISH huquqi bo'lmagan xodim faqat o'zi mas'ul bo'lgan
     * akkauntga ariza biriktira oladi — buni frontend dropdown'da ham cheklaymiz
     * (/admin/get-instagram-akkauntlar), lekin bu yerda ham tekshiramiz, chunki
     * so'rov to'g'ridan-to'g'ri (dropdown'ni chetlab) yuborilishi mumkin.
     */
    @Transactional
    public ApiResponse qoshish(ArizaSaveDto dto, Users joriyFoydalanuvchi) {
        if (dto.telefon() == null || dto.telefon().isBlank()) {
            return new ApiResponse("Telefon raqami kiritilishi shart", false);
        }

        InstagramAkkaunt akkaunt = null;
        if (dto.instagramAkkauntId() != null) {
            akkaunt = instagramAkkauntRepository.findById(dto.instagramAkkauntId()).orElse(null);
            if (akkaunt == null) {
                return new ApiResponse("Instagram akkaunt topilmadi", false);
            }

            boolean boshqarishHuquqi = joriyFoydalanuvchi.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("INSTAGRAM_AKKAUNT_BOSHQARISH"));
            boolean ozining = akkaunt.getMasulUser() != null
                    && akkaunt.getMasulUser().getId() == joriyFoydalanuvchi.getId();
            if (!boshqarishHuquqi && !ozining) {
                return new ApiResponse("Siz faqat o'zingiz mas'ul bo'lgan akkauntga ariza biriktira olasiz", false);
            }
        }

        Ariza ariza = new Ariza();
        ariza.setIsmFamiliya(dto.ismFamiliya() == null ? null : dto.ismFamiliya().trim());
        ariza.setTelefon(dto.telefon().trim());
        ariza.setIzoh(dto.izoh());
        ariza.setManba("Qo'lda kiritilgan");
        ariza.setInstagramAkkaunt(akkaunt);
        arizaRepository.save(ariza);

        tarixService.yoz("Ariza", "Qo'lda qo'shildi", ariza.getId(), ariza.getTelefon(), null);
        return new ApiResponse("Ariza qo'shildi", true);
    }

    @Transactional
    public ApiResponse holatniYangilash(Long id, ArizaHolatiSaveDto dto, Users joriyFoydalanuvchi) {
        Ariza ariza = arizaRepository.findById(id).orElse(null);
        if (ariza == null) {
            return new ApiResponse("Ariza topilmadi", false);
        }
        if (dto.holat() == null) {
            return new ApiResponse("Holat ko'rsatilishi shart", false);
        }

        ariza.setHolat(dto.holat());
        ariza.setIzoh(dto.izoh());
        ariza.setHolatOzgartirgan(joriyFoydalanuvchi);
        arizaRepository.save(ariza);

        tarixService.yoz("Ariza", "Holati o'zgartirildi", ariza.getId(),
                ariza.getTelefon(), "Yangi holat: " + ariza.getHolat().getNomi());
        return new ApiResponse("Ariza holati yangilandi", true);
    }

    private ArizaDto dto(Ariza a) {
        return new ArizaDto(a.getId(), a.getIsmFamiliya(), a.getTelefon(), a.getManba(),
                a.getInstagramAkkaunt() == null ? null : a.getInstagramAkkaunt().getNomi(),
                a.getFormId(), a.getAdId(), a.getHolat(),
                a.getHolat() == null ? null : a.getHolat().getNomi(),
                a.getIzoh(), a.getCreatedTime());
    }
}
