package com.elias.attendancecontrol.service;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
@Slf4j
@Service
public class QRGeneratorService {
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    public String generateQRCodeBase64(String content) {
        return generateQRCodeBase64(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    public String generateQRCodeBase64(String content, int width, int height) {
        try {
            log.debug("Generating QR code for content: {}", content);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(qrBytes);
            log.debug("QR code generated successfully");
            return "data:image/png;base64," + base64Image;
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Error al generar código QR", e);
        }
    }
    public String generateAttendanceQR(String verificationLink) {
        log.info("Generating attendance QR for link: {}", verificationLink);
        return generateQRCodeBase64(verificationLink);
    }
}
