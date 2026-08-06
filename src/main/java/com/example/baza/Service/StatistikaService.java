package com.example.baza.Service;

import com.example.baza.Dto.MagazinStatDto;
import com.example.baza.Dto.MahsulotDto;
import com.example.baza.Dto.OtkazmaDto;
import com.example.baza.Dto.SotuvDto;
import com.example.baza.Dto.StatistikaDto;
import com.example.baza.Entity.Rol;
import com.example.baza.Entity.SotuvHolati;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.KategoriyaRepository;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistika sahifasi uchun umumiy ko'rsatkichlarni hisoblaydi.
 *
 * Boshqa servislarning ALLAQACHON to'g'ri sozlangan scoping'idan (owner —
 * barchasi, hodim — faqat o'z magazini) foydalanadi, shuning uchun bu yerda
 * ruxsat/mansublik tekshiruvi qaytadan yozilmaydi — MahsulotService,
 * SotuvService, OtkazmaService'dagi mavjud metodlar chaqiriladi.
 *
 * Yangi statistika qo'shish uchun: shu metodga hisoblovni qo'shing va
 * StatistikaDto'ga mos maydon qo'shing — sahifa avtomatik yangi kartani
 * ko'rsatadi (statistika.html'ga ham karta qo'shilishi kerak).
 */
@Service
public class StatistikaService {

    private final MahsulotService mahsulotService;
    private final SotuvService sotuvService;
    private final OtkazmaService otkazmaService;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final KategoriyaRepository kategoriyaRepository;

    public StatistikaService(MahsulotService mahsulotService,
                             SotuvService sotuvService,
                             OtkazmaService otkazmaService,
                             MagazinRepository magazinRepository,
                             UsersRepository usersRepository,
                             KategoriyaRepository kategoriyaRepository) {
        this.mahsulotService = mahsulotService;
        this.sotuvService = sotuvService;
        this.otkazmaService = otkazmaService;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.kategoriyaRepository = kategoriyaRepository;
    }

    @Transactional(readOnly = true)
    public StatistikaDto hisobla(String username) {
        Users u = usersRepository.findByUsername(username).orElse(null);
        boolean owner = u != null && u.getRollar() != null
                && u.getRollar().stream().anyMatch(Rol::isTizimRoli);

        List<MahsulotDto> mahsulotlar = owner
                ? mahsulotService.getAllMahsulotlar()
                : mahsulotService.getMeningMahsulotlarim(username);
        List<SotuvDto> sotuvlar = sotuvService.getSotuvlar(username, null, null);
        List<OtkazmaDto> kutilayotganOtkazmalar = otkazmaService.kelayotganlar(username);
        Long userId = u == null ? null : u.getId();

        double umumiyKv = mahsulotlar.stream().mapToDouble(this::jamiKv).sum();
        long umumiyZavodQiymati = mahsulotlar.stream()
                .mapToLong(m -> m.zavodNarxi() == null ? 0 : m.zavodNarxi()).sum();

        List<SotuvDto> yakunlangan = sotuvlar.stream()
                .filter(s -> s.holat() == SotuvHolati.SOTILDI).toList();
        long sotuvSummasi = yakunlangan.stream().mapToLong(s -> s.summa() == null ? 0 : s.summa()).sum();
        long sotuvFoyda = yakunlangan.stream().mapToLong(s -> s.foyda() == null ? 0 : s.foyda()).sum();

        LocalDate bugun = LocalDate.now();
        List<SotuvDto> bugungi = yakunlangan.stream()
                .filter(s -> s.vaqt() != null && s.vaqt().toLocalDate().isEqual(bugun)).toList();
        long bugungiSummasi = bugungi.stream().mapToLong(s -> s.summa() == null ? 0 : s.summa()).sum();

        List<SotuvDto> katmda = sotuvlar.stream()
                .filter(s -> s.holat() == SotuvHolati.KATMDA).toList();
        long katmSummasi = katmda.stream().mapToLong(s -> s.summa() == null ? 0 : s.summa()).sum();

        List<MagazinStatDto> magazinlar = magazinlarBoyicha(owner, userId, mahsulotlar, yakunlangan);

        return new StatistikaDto(
                owner,
                mahsulotlar.size(),
                umumiyKv,
                umumiyZavodQiymati,
                owner ? magazinRepository.count() : magazinlar.size(),
                owner ? usersRepository.count() : null,
                kategoriyaRepository.count(),
                yakunlangan.size(),
                sotuvSummasi,
                sotuvFoyda,
                bugungi.size(),
                bugungiSummasi,
                katmda.size(),
                katmSummasi,
                kutilayotganOtkazmalar.size(),
                magazinlar);
    }

    /** Magazin bo'yicha taqsimot — owner uchun barcha magazinlar, hodim uchun faqat o'zi mas'ul bo'lganlari */
    private List<MagazinStatDto> magazinlarBoyicha(boolean owner, Long userId, List<MahsulotDto> mahsulotlar,
                                                    List<SotuvDto> yakunlangan) {
        Map<Long, String> nomlar = new LinkedHashMap<>();
        if (owner) {
            magazinRepository.findAll().forEach(m -> nomlar.put(m.getId(), m.getNomi()));
        } else if (userId != null) {
            magazinRepository.findByHodimlar_Id(userId).forEach(m -> nomlar.put(m.getId(), m.getNomi()));
        }

        return nomlar.entrySet().stream()
                .map(e -> {
                    Long magazinId = e.getKey();
                    List<MahsulotDto> mMahsulotlar = mahsulotlar.stream()
                            .filter(m -> magazinId.equals(m.magazinId())).toList();
                    List<SotuvDto> mSotuvlar = yakunlangan.stream()
                            .filter(s -> magazinId.equals(s.magazinId())).toList();

                    return new MagazinStatDto(
                            magazinId, e.getValue(),
                            mMahsulotlar.size(),
                            mMahsulotlar.stream().mapToDouble(this::jamiKv).sum(),
                            mMahsulotlar.stream().mapToLong(m -> m.zavodNarxi() == null ? 0 : m.zavodNarxi()).sum(),
                            mSotuvlar.size(),
                            mSotuvlar.stream().mapToLong(s -> s.summa() == null ? 0 : s.summa()).sum(),
                            mSotuvlar.stream().mapToLong(s -> s.foyda() == null ? 0 : s.foyda()).sum());
                })
                .sorted(Comparator.comparingLong(MagazinStatDto::sotuvSummasi).reversed())
                .toList();
    }

    /**
     * Bitta mahsulot qatorining JAMI kvadrati (dona soniga bog'liq):
     *   kv.metr -> miqdorning o'zi (allaqachon kv.metrda)
     *   dona    -> 1 donaning kvadrati × miqdor
     *   metr    -> kv tushunchasi yo'q (0)
     */
    private double jamiKv(MahsulotDto m) {
        if ("kv.metr".equals(m.birlik())) {
            return m.miqdor() == null ? 0 : m.miqdor();
        }
        if (MahsulotService.donami(m.birlik()) && m.kv() != null) {
            double dona = m.miqdor() == null || m.miqdor() <= 0 ? 1 : m.miqdor();
            return m.kv() * dona;
        }
        return 0;
    }
}
