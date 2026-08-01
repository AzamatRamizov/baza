package com.example.baza.Service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Exceldan (yoki CSV'dan) oddiy nomlar ustunini o'qish.
 *
 * Faylda sarlavha bo'lishi ham, bo'lmasligi ham mumkin — birinchi qator
 * "nomi", "kategoriya", "name" kabi so'z bo'lsa sarlavha deb tashlanadi.
 * Har bir qatordan BIRINCHI to'ldirilgan katak olinadi, shuning uchun
 * nomlar A ustunida bo'lishi shart emas.
 */
@Service
public class ExcelOqishService {

    /** Sarlavha qatorini aniqlash uchun (kichik harfda) */
    private static final Set<String> SARLAVHA_SOZLAR = Set.of(
            "nomi", "nom", "kategoriya", "kategoriya nomi", "kategoriyalar",
            "name", "title", "название", "наименование", "категория", "№", "t/r");

    private final DataFormatter formatter = new DataFormatter();

    /**
     * Fayldagi barcha bo'sh bo'lmagan nomlarni tartibi bilan qaytaradi
     * (takrorlar olib tashlanmaydi — chaqiruvchi tomon hal qiladi).
     *
     * @throws IllegalArgumentException fayl noto'g'ri yoki o'qib bo'lmasa
     */
    public List<String> nomlarniOqi(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fayl tanlanmagan yoki bo'sh");
        }

        String nom = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase();

        try (InputStream is = file.getInputStream()) {
            return nom.endsWith(".csv") ? csvOqi(is) : excelOqi(is);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Faylni o'qib bo'lmadi — .xlsx, .xls yoki .csv formatida ekanini tekshiring");
        }
    }

    // ================= EXCEL =================

    private List<String> excelOqi(InputStream is) throws Exception {
        List<String> natija = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Faylda birorta ham varaq yo'q");
            }
            Sheet sheet = workbook.getSheetAt(0);

            boolean birinchi = true;
            for (Row row : sheet) {
                String qiymat = qatordanOl(row);
                if (qiymat == null) continue;

                if (birinchi) {
                    birinchi = false;
                    if (sarlavhami(qiymat)) continue;   // sarlavha qatorini tashlaymiz
                }
                natija.add(qiymat);
            }
        }
        return natija;
    }

    /** Qatordagi birinchi to'ldirilgan katakning matni (bo'sh qator uchun null) */
    private String qatordanOl(Row row) {
        if (row == null) return null;
        for (int i = row.getFirstCellNum(); i >= 0 && i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null || cell.getCellType() == CellType.BLANK) continue;

            String matn = formatter.formatCellValue(cell);
            matn = tozala(matn);
            if (!matn.isEmpty() && !faqatRaqammi(matn)) {
                return matn;
            }
        }
        return null;
    }

    /** Tartib raqami ustunini (1, 2, 3...) nom deb olmaslik uchun */
    private boolean faqatRaqammi(String matn) {
        return matn.matches("\\d+([.,]\\d+)?");
    }

    // ================= CSV =================

    private List<String> csvOqi(InputStream is) throws Exception {
        List<String> natija = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String qator;
            boolean birinchi = true;
            while ((qator = reader.readLine()) != null) {
                // BOM va ajratgichlar
                qator = qator.replace("\uFEFF", "");
                String qiymat = null;
                for (String bolak : qator.split("[;,\\t]")) {
                    String t = tozala(bolak.replace("\"", ""));
                    if (!t.isEmpty() && !faqatRaqammi(t)) {
                        qiymat = t;
                        break;
                    }
                }
                if (qiymat == null) continue;

                if (birinchi) {
                    birinchi = false;
                    if (sarlavhami(qiymat)) continue;
                }
                natija.add(qiymat);
            }
        }
        return natija;
    }

    // ================= YORDAMCHI =================

    private boolean sarlavhami(String qiymat) {
        return SARLAVHA_SOZLAR.contains(qiymat.toLowerCase());
    }

    /** Ortiqcha bo'shliqlar, ko'rinmas belgilar olib tashlanadi */
    private String tozala(String matn) {
        if (matn == null) return "";
        return matn.replace('\u00A0', ' ')      // no-break space
                .replaceAll("\\s+", " ")
                .trim();
    }
}