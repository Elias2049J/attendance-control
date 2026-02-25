package com.elias.attendancecontrol.service.implementation;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
public class QRGeneratorService {
    @Value("${qr.default-width}")
    private int defaultWidth;
    @Value("${qr.default-height}")
    private int defaultHeight;

    public String generateQRCodeBase64(String content) {
        try {
            log.debug("Generating QR code for content: {}", content);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, defaultWidth, defaultHeight);
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
