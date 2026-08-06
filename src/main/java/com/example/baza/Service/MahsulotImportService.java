package com.example.baza.Service;

import com.example.baza.Dto.MahsulotImportNatijaDto;
import com.example.baza.Entity.Kategoriya;
import com.example.baza.Entity.Magazin;
import com.example.baza.Entity.Mahsulot;
import com.example.baza.Entity.Users;
import com.example.baza.Repository.KategoriyaRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mahsulotlarni Excel (POS eksporti) fayldan ommaviy import qilish.
 *
 * KUTILADIGAN USTUNLAR (sarlavha bo'yicha topiladi, tartib muhim emas):
 *   "Товар"                    -> nomi / kodi / o'lcham (quyida)
 *   "Категория"                -> kategoriya (yo'q bo'lsa avtomatik yaratiladi)
 *   "Остаток"                  -> Metr uchun bo'yi, Штук uchun miqdor (dona).
 *                                 BO'SH YOKI 0 BO'LSA — qator BUTUNLAY o'tkazib yuboriladi
 *                                 (qoldiqsiz mahsulot qo'shilmaydi).
 *   "Единица по умолчанию"     -> "Метр" -> turi=metraj, "Штук" -> turi=gatoviy
 *
 * "Товар" USTUNI TAHLILI:
 *   Metr (masalan "YEC LUNA S211H (3)"):
 *     qavs ichidagi son -> eni, qavsdan oldingi so'z -> kodi, undan oldingilari -> nomi.
 *     Qoida topilmasa (masalan qavs teskari tartibda yoki umuman yo'q) -> kod/eni bo'sh
 *     qoladi, qator o'tkazib yuboriladi (natijada sababi bilan ko'rsatiladi).
 *
 *   Штук (masalan "SULTAN 3.5X7.30 KUSOK", "TEHRON 4011 3.5X4"):
 *     "ENIxBOYI" ko'rinishidagi son juftligi -> eni/bo'yi. Shu juftlikka BEVOSITA
 *     tegib turgan BITTA so'z (chapda yoki o'ngda) -> kodi, qolgani -> nomi.
 *     Ikkala tomonda ham so'z bo'lsa — o'ngdagisi BITTA so'z bo'lsa o'sha kod
 *     (chunki "NOMI [ko'p so'z] O'LCHAM KOD" eng ko'p uchraydigan shakl), aks holda
 *     kod bo'sh qoladi va butun qolgan matn nomi bo'ladi.
 *
 * KOD TAKRORLANISHI: dizayn kodi (masalan "4564") ko'pincha bir nechta o'lchamda
 * (turli eni/bo'yida) qayta-qayta uchraydi — bular HAQIQATDA alohida mahsulotlar,
 * shuning uchun BIR XIL kod bilan bir nechtasi qo'shilishi taqiqlanmaydi (odatiy
 * qo'lda qo'shishdan farqli o'laroq — bu yerda foydalanuvchi so'rovi bilan ataylab
 * shunday qilingan).
 */
@Service
public class MahsulotImportService {

    private static final int MAX_QATOR = 20000;
    private static final int MAX_XATO_KORSATISH = 300;

    /** Sarlavhadagi ustun nomlari (kichik harfda, bo'shliqlarsiz solishtiriladi) */
    private static final String H_TOVAR = "товар";
    private static final String H_KATEGORIYA = "категория";
    private static final String H_OSTATOK = "остаток";
    private static final String H_BIRLIK = "единица по умолчанию";

    /** "SO'Z (SON)" — Metr uchun: qavsdan oldingi so'z kod, ichidagi son eni */
    private static final Pattern METR_PAT =
            Pattern.compile("([^\\s()]+)\\s*\\(\\s*(\\d+(?:[.,]\\d+)?)\\s*\\)");

    /** "SON x SON" — Штук uchun eni/bo'yi (qavs ichida yoki ichida bo'lmasin) */
    private static final Pattern XL_PAT =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*[xXхХ]\\s*(\\d+(?:[.,]\\d+)?)");

    private final MahsulotRepository mahsulotRepository;
    private final KategoriyaRepository kategoriyaRepository;
    private final MagazinRepository magazinRepository;
    private final UsersRepository usersRepository;
    private final TarixService tarixService;

    private final DataFormatter formatter = new DataFormatter();

    public MahsulotImportService(MahsulotRepository mahsulotRepository,
                                 KategoriyaRepository kategoriyaRepository,
                                 MagazinRepository magazinRepository,
                                 UsersRepository usersRepository,
                                 TarixService tarixService) {
        this.mahsulotRepository = mahsulotRepository;
        this.kategoriyaRepository = kategoriyaRepository;
        this.magazinRepository = magazinRepository;
        this.usersRepository = usersRepository;
        this.tarixService = tarixService;
    }

    @Transactional
    public MahsulotImportNatijaDto importQil(MultipartFile file, Long magazinId) {
        if (file == null || file.isEmpty()) {
            return MahsulotImportNatijaDto.xato("Fayl tanlanmagan yoki bo'sh");
        }

        Magazin magazin = null;
        if (magazinId != null) {
            magazin = magazinRepository.findById(magazinId).orElse(null);
            if (magazin == null) {
                return MahsulotImportNatijaDto.xato("Tanlangan magazin topilmadi");
            }
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Users yaratgan = usersRepository.findByUsername(username).orElse(null);

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
                        "Kerakli ustun topilmadi: \"" + yoqolgan + "\" — faylning sarlavha " +
                                "qatorida \"Товар\", \"Категория\", \"Остаток\" va " +
                                "\"Единица по умолчанию\" ustunlari bo'lishi shart");
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

        // Kategoriyalar keshi — nomi bo'yicha, bitta faylda bir xil kategoriya ko'p marta
        // uchrasa ham bitta marta yaratiladi
        Map<String, Kategoriya> kategoriyaKesh = new HashMap<>();
        for (Kategoriya k : kategoriyaRepository.findAll()) {
            if (k.getNomi() != null) kategoriyaKesh.put(kalit(k.getNomi()), k);
        }
        List<Kategoriya> yangiKategoriyalar = new ArrayList<>();

        int oqildi = 0;
        int kodSanogichi = 0;
        List<Mahsulot> yangiMahsulotlar = new ArrayList<>();
        List<String> xatolar = new ArrayList<>();

        for (Row row : rows) {
            String tovar = katakMatn(row, ustunlar.get(H_TOVAR));
            if (tovar.isEmpty()) continue;
            oqildi++;
            int excelQator = row.getRowNum() + 1;

            String birlikRaw = katakMatn(row, ustunlar.get(H_BIRLIK));
            String kategoriyaRaw = katakMatn(row, ustunlar.get(H_KATEGORIYA));
            Double ostatok = katakSon(row, ustunlar.get(H_OSTATOK));

            QatorNatija qn;
            if (birlikRaw.equalsIgnoreCase("Метр")) {
                qn = metrTahlil(tovar, ostatok);
            } else if (birlikRaw.equalsIgnoreCase("Штук")) {
                qn = shtukTahlil(tovar, ostatok);
            } else {
                xatolar.add("Qator " + excelQator + ": noma'lum birlik \"" + birlikRaw +
                        "\" (\"" + tovar + "\") — o'tkazib yuborildi");
                continue;
            }

            String xato = qn.tekshir();
            if (xato != null) {
                xatolar.add("Qator " + excelQator + ": " + xato + " (\"" + tovar + "\")");
                continue;
            }

            Kategoriya kategoriya = null;
            if (!kategoriyaRaw.isEmpty()) {
                String kKalit = kalit(kategoriyaRaw);
                kategoriya = kategoriyaKesh.get(kKalit);
                if (kategoriya == null) {
                    kategoriya = new Kategoriya();
                    kategoriya.setNomi(kategoriyaRaw);
                    kategoriyaKesh.put(kKalit, kategoriya);
                    yangiKategoriyalar.add(kategoriya);
                }
            }

            Mahsulot m = new Mahsulot();
            m.setNomi(qn.nomi);
            m.setKod(qn.kod);
            m.setKategoriya(kategoriya);
            m.setTuri(qn.turi);
            m.setBirlik(MahsulotService.birlikniAniqla(qn.turi, qn.eni));
            m.setBoyi(qn.boyi);
            m.setEni(qn.eni);
            m.setKv(kvHisobla(qn.boyi, qn.eni));
            m.setMiqdor(qn.miqdorHisobla());
            m.setMagazin(magazin);
            m.setYaratganUser(yaratgan);

            yangiMahsulotlar.add(m);
            kodSanogichi++;
        }

        if (!yangiKategoriyalar.isEmpty()) {
            kategoriyaRepository.saveAll(yangiKategoriyalar);
        }
        if (!yangiMahsulotlar.isEmpty()) {
            mahsulotRepository.saveAll(yangiMahsulotlar);
        }

        boolean qisqartirilgan = xatolar.size() > MAX_XATO_KORSATISH;
        List<String> korsatilganXatolar = qisqartirilgan
                ? xatolar.subList(0, MAX_XATO_KORSATISH) : xatolar;

        tarixService.yoz("Mahsulot", "Exceldan yuklandi", null, file.getOriginalFilename(),
                "O'qildi: " + oqildi + " | Qo'shildi: " + kodSanogichi +
                        " | O'tkazib yuborildi: " + xatolar.size() +
                        (magazin == null ? "" : " | Magazin: " + magazin.getNomi()));

        String xulosa = kodSanogichi == 0
                ? "Birorta ham mahsulot qo'shilmadi"
                : kodSanogichi + " ta mahsulot qo'shildi";
        if (!xatolar.isEmpty()) {
            xulosa += ", " + xatolar.size() + " tasi o'tkazib yuborildi";
        }

        return new MahsulotImportNatijaDto(true, xulosa, oqildi, kodSanogichi,
                xatolar.size(), korsatilganXatolar, qisqartirilgan);
    }

    // ================= "Товар" USTUNI TAHLILI =================

    /** Bitta qatorni tahlil qilish natijasi — hali tekshirilmagan, tayyor bo'lmasligi mumkin */
    private static final class QatorNatija {
        String nomi;
        String kod;      // null bo'lishi mumkin
        Double eni;      // null bo'lishi mumkin
        Double boyi;      // metrda — Остаток (qoldiq, bo'sh bo'lsa 0); shtukda — ENIxBOYI dan
        Double miqdor;    // faqat shtuk uchun (necha dona, Остаток'dan) — bo'sh/0 bo'lsa o'tkazib yuboriladi
        String turi;      // "metraj" | "gatoviy"

        /**
         * Mahsulotning MIQDOR maydoni (birlikda):
         *   gatoviy  -> necha dona (Остаток) — bo'sh/0 bo'lsa tekshir() da qator o'tkazib yuboriladi
         *   kv.metr  -> bo'yi × eni (kv) — MahsulotService.miqdorniAniqla bilan bir xil qoida
         *   metr     -> bo'yi (eni bo'lmasa)
         */
        Double miqdorHisobla() {
            if ("gatoviy".equals(turi)) {
                return miqdor == null ? null : Math.round(miqdor * 100.0) / 100.0;
            }
            if (eni != null && eni > 0 && boyi != null) {
                return Math.round(boyi * eni * 100.0) / 100.0;
            }
            return boyi;
        }

        /** null qaytarsa — qator to'g'ri, aks holda shu matn sabab sifatida ko'rsatiladi */
        String tekshir() {
            if (nomi == null || nomi.isBlank()) return "nomi aniqlanmadi";
            if (kod == null || kod.isBlank()) return "kodi aniqlanmadi";
            if (eni == null || eni <= 0) {
                return "eni aniqlanmadi (narx 1 kv.metr bo'yicha hisoblanadi)";
            }
            // bo'yi: metrda Остаток'dan, shtukda Товар'dagi ENIxBOYI'dan — ikkalasida ham kerak
            if (boyi == null || boyi <= 0) {
                return "gatoviy".equals(turi)
                        ? "bo'yi aniqlanmadi"
                        : "bo'yi aniqlanmadi (Остаток bo'sh yoki 0)";
            }
            if ("gatoviy".equals(turi)) {
                // Штук uchun Остаток -> miqdor (necha dona); bo'sh yoki 0 bo'lsa o'tkazib yuboriladi
                if (miqdor == null || miqdor <= 0) {
                    return "miqdor aniqlanmadi (Остаток bo'sh yoki 0)";
                }
                double d = miqdorHisobla();
                if (Math.abs(d - Math.round(d)) > 0.0001) {
                    return "dona miqdori butun son emas: " + d;
                }
            }
            return null;
        }
    }

    /**
     * Metr qatorlar: "NOMI KOD (ENI)" — qavsdan oldingi so'z kod, ichidagi son eni.
     * Qoidaga to'g'ri kelmasa (masalan teskari tartib yoki qavs yo'q) — kod/eni bo'sh qoladi.
     */
    private QatorNatija metrTahlil(String tovar, Double ostatok) {
        QatorNatija qn = new QatorNatija();
        qn.turi = "metraj";
        // Остаток bo'sh bo'lsa bu qator o'tkazib yuboriladi (tekshir() da boyi<=0 xato beradi)
        qn.boyi = ostatok;

        Matcher m = METR_PAT.matcher(tovar);
        Matcher oxirgi = null;
        int oxirgiStart = -1;
        String oxirgiKod = null, oxirgiEni = null;
        while (m.find()) {
            oxirgi = m;
            oxirgiStart = m.start(1);
            oxirgiKod = m.group(1);
            oxirgiEni = m.group(2);
        }

        if (oxirgi != null) {
            qn.kod = oxirgiKod;
            qn.eni = sonAylantir(oxirgiEni);
            qn.nomi = tovar.substring(0, oxirgiStart).trim();
        } else {
            qn.nomi = tovar.trim();
        }
        return qn;
    }

    /**
     * Штук qatorlar: "NOMI [KOD] ENIxBOYI [KOD]" — o'lchamga BEVOSITA tegib turgan
     * bitta so'z kod (chapda yoki o'ngda). Ikkala tomonda ham so'z bo'lsa — aniq emas,
     * kod bo'sh qoladi va butun qolgan matn nomi bo'ladi.
     */
    private QatorNatija shtukTahlil(String tovar, Double ostatok) {
        QatorNatija qn = new QatorNatija();
        qn.turi = "gatoviy";
        qn.miqdor = ostatok;

        Matcher xm = XL_PAT.matcher(tovar);
        if (!xm.find()) {
            qn.nomi = tovar.trim();
            return qn;
        }

        int start = xm.start();
        int end = xm.end();
        // qavs ichida bo'lsa qavslarni ham qo'shib olib tashlaymiz
        if (start > 0 && tovar.charAt(start - 1) == '(') start--;
        if (end < tovar.length() && tovar.charAt(end) == ')') end++;

        qn.eni = sonAylantir(xm.group(1));
        qn.boyi = sonAylantir(xm.group(2));

        String oldin = tovar.substring(0, start).trim();
        String keyin = tovar.substring(end).trim();
        boolean borOldin = !oldin.isEmpty();
        boolean borKeyin = !keyin.isEmpty();

        if (borOldin && borKeyin) {
            // O'lcham O'RTADA: "NOMI [ko'p so'z] O'LCHAM KOD" — eng ko'p uchraydigan shakl.
            // O'ngdagi BITTA so'z bo'lsa — shu kod, chapdagi to'liq matn esa nomi.
            String[] keyinSoz = keyin.split("\\s+");
            if (keyinSoz.length == 1) {
                qn.kod = keyin;
                qn.nomi = oldin;
            } else {
                qn.nomi = (oldin + " " + keyin).trim();
            }
        } else if (borOldin) {
            String[] soz = oldin.split("\\s+");
            if (soz.length >= 2) {
                qn.kod = soz[soz.length - 1];
                qn.nomi = String.join(" ", java.util.Arrays.copyOf(soz, soz.length - 1));
            } else {
                qn.nomi = oldin;
            }
        } else if (borKeyin) {
            qn.nomi = keyin;
        } else {
            qn.nomi = tovar.trim();
        }
        return qn;
    }

    private Double kvHisobla(Double boyi, Double eni) {
        if (boyi == null || eni == null) return null;
        return Math.round(boyi * eni * 100.0) / 100.0;
    }

    private Double sonAylantir(String matn) {
        if (matn == null) return null;
        try {
            return Double.parseDouble(matn.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ================= FAYL O'QISH YORDAMCHILARI =================

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
        for (String kerak : List.of(H_TOVAR, H_KATEGORIYA, H_OSTATOK, H_BIRLIK)) {
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
        return matn.isEmpty() ? null : sonAylantir(matn);
    }

    private String kalit(String nomi) {
        return nomi.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String tozala(String matn) {
        if (matn == null) return "";
        return matn.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }
}
