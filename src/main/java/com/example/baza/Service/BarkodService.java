package com.example.baza.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/** QR kod va shtrix-kod (Code128) rasm generatsiyasi — mahsulot detail sahifasi uchun. */
@Service
public class BarkodService {

    public byte[] qrKod(String matn, int hajm) throws WriterException, IOException {
        BitMatrix matritsa = new MultiFormatWriter().encode(
                matn, BarcodeFormat.QR_CODE, hajm, hajm,
                Map.of(EncodeHintType.MARGIN, 1));
        return pngga(matritsa);
    }

    public byte[] shtrixKod(String matn, int kengligi, int balandligi) throws WriterException, IOException {
        BitMatrix matritsa = new MultiFormatWriter().encode(
                matn, BarcodeFormat.CODE_128, kengligi, balandligi,
                Map.of(EncodeHintType.MARGIN, 5));
        return pngga(matritsa);
    }

    private byte[] pngga(BitMatrix matritsa) throws IOException {
        ByteArrayOutputStream chiqish = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matritsa, "PNG", chiqish);
        return chiqish.toByteArray();
    }
}
