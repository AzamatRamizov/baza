package com.example.baza.Service;

import com.example.baza.Dto.ApiResponse;
import com.example.baza.Dto.KutilayotganMahsulotDto;
import com.example.baza.Dto.MahsulotImportNatijaDto;
import com.example.baza.Dto.MahsulotSaveDto;
import com.example.baza.Entity.KutilayotganMahsulot;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.KutilayotganMahsulotRepository;
import com.example.baza.Repository.MagazinRepository;
import com.example.baza.Repository.MahsulotRepository;
import com.example.baza.Repository.UsersRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Zavod "Chiqim" (jo'natma) Excel faylini yuklab, mahsulotga DARHOL qo'shmasdan
 * "KUTILMOQDA" holatida saqlaydigan ombor. Fizik tekshiruv (skaner bilan seriya
 * raqamini o'qish) orqali belgilangan qatorlargina "Tasdiqlash" bosilganda haqiqiy
 * Mahsulotga aylanadi.
 *
 * KUTILADIGAN USTUNLAR (sarlavha bo'yicha topiladi):
 *   "Etiket Adı" -> nomi, "DIZAYN" -> kodi, "EN"/"BOY" -> o'lcham (sm, metrga
 *   aylantiriladi), "DONA" -> miqdor, "Serial №" -> seriya raqami (takrorlanmas,
 *   fizik mahsulotdagi shtrix-kod shu).
 *   "Zavod", "BAR CODE", "Şekil Adı", "Rangi" — e'tiborga olinmaydi.
 */
@Service
public class KutilayotganMahsulotService {

    private static final int MAX_QATOR = 5000;
    private static final int MAX_XATO_KORSATISH = 300;
    private static final String KUTILMOQDA = "KUTILMOQDA";
    private static final String TASDIQLANDI = "TASDIQLANDI";

    private static final String H_NOMI = "etiket adı";
    private static final String H_KOD = "dizayn";
    private static final String H_EN = "en";
    private static final String H_BOY = "boy";
    private static final String H_DONA = "dona";
    private static final String H_SERIAL = "serial №";

    private final KutilayotganMahsulotRepository kutilayotganRepository;
    private final MahsulotRepository mahsulotRepository;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final MahsulotService mahsulotService;
    private final TarixService tarixService;

    private final DataFormatter formatter = new DataFormatter();

    public KutilayotganMahsulotService(KutilayotganMahsulotRepository kutilayotganRepository,
                                       MahsulotRepository mahsulotRepository,
                                       MagazinRepository magazinRepository,
                                       UsersRepository usersRepository,
                                       MahsulotService mahsulotService,
                                       TarixService tarixService) {
        this.kutilayotganRepository = kutilayotganRepository;
        this.mahsulotRepository = mahsulotRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.mahsulotService = mahsulotService;
        this.tarixService = tarixService;
    }

    // ================= YUKLASH =================

    @Transactional
    public MahsulotImportNatijaDto excelniYukla(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return MahsulotImportNatijaDto.xato("Fayl tanlanmagan yoki bo'sh");
        }

        Users yuklagan = joriyUser();

        List<Row> rows;
        Map<String, Integer> ustunlar;
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            if (workbook.getNumberOfSheets() == 0) {
                return MahsulotImportNatijaDto.xato("Faylda birorta ham varaq yo'q");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row sarlavha = sheet.getRow(sheet.getFirstRowNum());
            if (sarlavha == null) {
                return MahsulotImportNatijaDto.xato("Faylda sarlavha qatori topilmadi");
            }

            ustunlar = ustunlarniTop(sarlavha);
            String yoqolgan = talabQilinganUstunlarTekshir(ustunlar);
            if (yoqolgan != null) {
                return MahsulotImportNatijaDto.xato(
                        "Kerakli ustun topilmadi: \"" + yoqolgan + "\" — faylning sarlavha qatorida " +
                                "\"Etiket Adı\", \"DIZAYN\", \"EN\", \"BOY\", \"DONA\" va \"Serial №\" " +
                                "ustunlari bo'lishi shart");
            }

            rows = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == sarlavha.getRowNum()) continue;
                rows.add(row);
            }
        } catch (Exception e) {
            return MahsulotImportNatijaDto.xato(
                    "Faylni o'qib bo'lmadi — .xlsx yoki .xls formatida ekanini tekshiring");
        }

        if (rows.size() > MAX_QATOR) {
            return MahsulotImportNatijaDto.xato(
                    "Faylda juda ko'p qator (" + rows.size() + "). " +
                            "Bir martada eng ko'pi " + MAX_QATOR + " ta bo'lishi mumkin");
        }

        int oqildi = 0;
        int qoshildi = 0;
        List<String> xatolar = new ArrayList<>();
        List<KutilayotganMahsulot> yangilar = new ArrayList<>();
        // Shu fayl ichida takrorlangan seriyalarni tutish uchun
        Map<String, Integer> faylSeriyalari = new HashMap<>();

        for (Row row : rows) {
            String nomi = katakMatn(row, ustunlar.get(H_NOMI));
            String kod = katakMatn(row, ustunlar.get(H_KOD));
            String serial = katakMatn(row, ustunlar.get(H_SERIAL));
            if (nomi.isEmpty() && kod.isEmpty() && serial.isEmpty()) continue; // butunlay bo'sh qator
            oqildi++;
            int excelQator = row.getRowNum() + 1;

            if (nomi.isEmpty()) {
                xatolar.add("Qator " + excelQator + ": nomi (Etiket Adı) bo'sh"); continue;
            }
            if (kod.isEmpty()) {
                xatolar.add("Qator " + excelQator + ": kodi (DIZAYN) bo'sh"); continue;
            }
            if (serial.isEmpty()) {
                xatolar.add("Qator " + excelQator + ": seriya raqami (Serial №) bo'sh"); continue;
            }

            Double enSm = katakSon(row, ustunlar.get(H_EN));
            Double boySm = katakSon(row, ustunlar.get(H_BOY));
            Double dona = katakSon(row, ustunlar.get(H_DONA));

            if (enSm == null || enSm <= 0 || boySm == null || boySm <= 0) {
                xatolar.add("Qator " + excelQator + ": EN/BOY o'lchami noto'g'ri (\"" + nomi + "\")"); continue;
            }
            if (dona == null || dona <= 0) {
                xatolar.add("Qator " + excelQator + ": DONA bo'sh yoki 0 (\"" + nomi + "\")"); continue;
            }

            if (faylSeriyalari.containsKey(serial.toLowerCase(Locale.ROOT))) {
                xatolar.add("Qator " + excelQator + ": seriya raqami \"" + serial +
                        "\" shu faylda " + faylSeriyalari.get(serial.toLowerCase(Locale.ROOT)) +
                        "-qatorda ham bor edi — o'tkazib yuborildi");
                continue;
            }
            if (mahsulotRepository.findBySerialKodIgnoreCase(serial).size() > 0) {
                xatolar.add("Qator " + excelQator + ": seriya raqami \"" + serial +
                        "\" bazada allaqachon mavjud (mahsulotga qo'shilgan)");
                continue;
            }
            if (kutilayotganRepository.existsByHolatAndSerialKodIgnoreCase(KUTILMOQDA, serial)) {
                xatolar.add("Qator " + excelQator + ": seriya raqami \"" + serial +
                        "\" allaqachon kutilmoqda ro'yxatida bor");
                continue;
            }
            faylSeriyalari.put(serial.toLowerCase(Locale.ROOT), excelQator);

            double eniM = MahsulotService.yaxlit(enSm / 100.0);
            double boyiM = MahsulotService.yaxlit(boySm / 100.0);

            KutilayotganMahsulot k = new KutilayotganMahsulot();
            k.setNomi(nomi);
            k.setKod(kod);
            k.setSerialKod(serial);
            k.setEni(eniM);
            k.setBoyi(boyiM);
            k.setKv(MahsulotService.yaxlit(eniM * boyiM));
            k.setMiqdor(MahsulotService.yaxlit(dona));
            k.setTuri("gatoviy");
            k.setHolat(KUTILMOQDA);
            k.setSkanerlandi(false);
            k.setYuklagan(yuklagan);
            k.setYuklanganVaqt(LocalDateTime.now());
            k.setFaylNomi(file.getOriginalFilename());

            yangilar.add(k);
            qoshildi++;
        }

        if (!yangilar.isEmpty()) {
            kutilayotganRepository.saveAll(yangilar);
        }

        boolean qisqartirilgan = xatolar.size() > MAX_XATO_KORSATISH;
        List<String> korsatilganXatolar = qisqartirilgan ? xatolar.subList(0, MAX_XATO_KORSATISH) : xatolar;

        tarixService.yoz("Mahsulot", "Kutilmoqdaga yuklandi", null, file.getOriginalFilename(),
                "O'qildi: " + oqildi + " | Qo'shildi: " + qoshildi +
                        " | O'tkazib yuborildi: " + xatolar.size());

        String xulosa = qoshildi == 0
                ? "Birorta ham qator qo'shilmadi"
                : qoshildi + " ta qator \"kutilmoqda\" ro'yxatiga qo'shildi";
        if (!xatolar.isEmpty()) {
            xulosa += ", " + xatolar.size() + " tasi o'tkazib yuborildi";
        }

        return new MahsulotImportNatijaDto(true, xulosa, oqildi, qoshildi,
                xatolar.size(), korsatilganXatolar, qisqartirilgan);
    }

    // ================= RO'YXAT / SKANER / TASDIQLASH =================

    @Transactional(readOnly = true)
    public List<KutilayotganMahsulotDto> royxat() {
        return kutilayotganRepository.findByHolatOrderByIdDesc(KUTILMOQDA).stream()
                .map(this::dto)
                .toList();
    }

    /** Fizik mahsulot skanerlanganda (seriya raqami o'qilganda) chaqiriladi — ptichka qo'yiladi */
    @Transactional
    public ApiResponse skanerBelgila(String serialKod) {
        if (serialKod == null || serialKod.isBlank()) {
            return new ApiResponse("Seriya raqami bo'sh", false);
        }
        String toza = serialKod.trim();

        List<KutilayotganMahsulot> topilganlar =
                kutilayotganRepository.findByHolatAndSerialKodIgnoreCase(KUTILMOQDA, toza);
        if (topilganlar.isEmpty()) {
            return new ApiResponse("Bu seriya raqami \"kutilmoqda\" ro'yxatida topilmadi: " + toza, false);
        }

        KutilayotganMahsulot k = topilganlar.stream()
                .filter(x -> !Boolean.TRUE.equals(x.getSkanerlandi()))
                .findFirst().orElse(topilganlar.get(0));

        if (Boolean.TRUE.equals(k.getSkanerlandi())) {
            return new ApiResponse(k.getNomi() + " (" + k.getKod() + ") — allaqachon belgilangan", true);
        }

        k.setSkanerlandi(true);
        k.setSkanerlagan(joriyUser());
        k.setSkanerlanganVaqt(LocalDateTime.now());
        kutilayotganRepository.save(k);

        return new ApiResponse(k.getNomi() + " (" + k.getKod() + ") belgilandi", true);
    }

    /** Ro'yxatdagi ptichkani qo'lda ham belgilash/bekor qilish mumkin (skaner ishlamay qolsa) */
    @Transactional
    public ApiResponse qolBelgila(Long id, boolean holat) {
        KutilayotganMahsulot k = kutilayotganRepository.findById(id).orElse(null);
        if (k == null || !KUTILMOQDA.equals(k.getHolat())) {
            return new ApiResponse("Qator topilmadi", false);
        }
        k.setSkanerlandi(holat);
        if (holat) {
            k.setSkanerlagan(joriyUser());
            k.setSkanerlanganVaqt(LocalDateTime.now());
        } else {
            k.setSkanerlagan(null);
            k.setSkanerlanganVaqt(null);
        }
        kutilayotganRepository.save(k);
        return new ApiResponse("Belgilandi", true);
    }

    @Transactional
    public ApiResponse ochirish(Long id) {
        KutilayotganMahsulot k = kutilayotganRepository.findById(id).orElse(null);
        if (k == null) {
            return new ApiResponse("Qator topilmadi", false);
        }
        kutilayotganRepository.delete(k);
        return new ApiResponse("O'chirildi", true);
    }

    /**
     * "Tasdiqlash" — FAQAT skanerlangan (ptichkali) qatorlar haqiqiy Mahsulotga
     * aylanadi (MahsulotService.addMahsulot orqali — bir xil kod/o'lcham/magazin
     * bo'lsa qoldiqqa qo'shiladi, aks holda yangi qator ochiladi), magazin shu
     * yerda belgilanadi. Skanerlanmagan qatorlar "kutilmoqda"da qolaveradi.
     */
    @Transactional
    public ApiResponse tasdiqlaHammasi(Long magazinId) {
        if (magazinId == null) {
            return new ApiResponse("Magazinni tanlang", false);
        }
        Magazin magazin = magazinRepository.findById(magazinId).orElse(null);
        if (magazin == null) {
            return new ApiResponse("Tanlangan magazin topilmadi", false);
        }

        List<KutilayotganMahsulot> skanerlanganlar = kutilayotganRepository
                .findByHolatOrderByIdDesc(KUTILMOQDA).stream()
                .filter(k -> Boolean.TRUE.equals(k.getSkanerlandi()))
                .toList();

        if (skanerlanganlar.isEmpty()) {
            return new ApiResponse("Belgilangan (skanerlangan) qator yo'q", false);
        }

        int qoshildi = 0;
        for (KutilayotganMahsulot k : skanerlanganlar) {
            MahsulotSaveDto dto = new MahsulotSaveDto(
                    k.getNomi(), k.getKod(), k.getSerialKod(), null, null, null,
                    k.getBoyi(), k.getEni(), k.getMiqdor(), k.getTuri(), magazinId);
            mahsulotService.addMahsulot(dto);

            k.setHolat(TASDIQLANDI);
            k.setMagazin(magazin);
            kutilayotganRepository.save(k);
            qoshildi++;
        }

        tarixService.yoz("Mahsulot", "Kutilmoqdadan tasdiqlandi", null,
                qoshildi + " ta mahsulot", "Magazin: " + magazin.getNomi());

        return new ApiResponse(qoshildi + " ta mahsulot \"" + magazin.getNomi() + "\" magaziniga qo'shildi", true);
    }

    // ================= YORDAMCHI =================

    private Users joriyUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findByUsername(username).orElse(null);
    }

    private KutilayotganMahsulotDto dto(KutilayotganMahsulot k) {
        return new KutilayotganMahsulotDto(
                k.getId(), k.getNomi(), k.getKod(), k.getSerialKod(),
                k.getBoyi(), k.getEni(), k.getKv(), k.getMiqdor(), k.getTuri(),
                k.getHolat(), k.getSkanerlandi(),
                k.getSkanerlagan() == null ? null : k.getSkanerlagan().getFish(),
                k.getSkanerlanganVaqt(),
                k.getYuklagan() == null ? null : k.getYuklagan().getFish(),
                k.getYuklanganVaqt(), k.getFaylNomi());
    }

    private Map<String, Integer> ustunlarniTop(Row sarlavha) {
        Map<String, Integer> natija = new HashMap<>();
        for (int i = sarlavha.getFirstCellNum(); i >= 0 && i < sarlavha.getLastCellNum(); i++) {
            Cell cell = sarlavha.getCell(i);
            if (cell == null) continue;
            String nomi = tozala(formatter.formatCellValue(cell)).toLowerCase(Locale.ROOT);
            if (!nomi.isEmpty()) natija.put(nomi, i);
        }
        return natija;
    }

    private String talabQilinganUstunlarTekshir(Map<String, Integer> ustunlar) {
        for (String kerak : List.of(H_NOMI, H_KOD, H_EN, H_BOY, H_DONA, H_SERIAL)) {
            if (!ustunlar.containsKey(kerak)) return kerak;
        }
        return null;
    }

    private String katakMatn(Row row, Integer ustun) {
        if (ustun == null) return "";
        Cell cell = row.getCell(ustun);
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        return tozala(formatter.formatCellValue(cell));
    }

    private Double katakSon(Row row, Integer ustun) {
        if (ustun == null) return null;
        Cell cell = row.getCell(ustun);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String matn = tozala(formatter.formatCellValue(cell));
        if (matn.isEmpty()) return null;
        try {
            return Double.parseDouble(matn.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String tozala(String matn) {
        if (matn == null) return "";
        return matn.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }
}
