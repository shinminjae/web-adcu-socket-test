package com.localtest.service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.stereotype.Service;

@Service
public class AccessKeyService {
    private static final String ORIGINAL_KEY = "mobilus"; // 7 bytes
    private static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";

    public String generateAccessKey(String deviceId, String vinId) throws Exception {
        byte[] keyBytes = Arrays.copyOf(ORIGINAL_KEY.getBytes(StandardCharsets.UTF_8), 32);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long timestamp = System.currentTimeMillis();
        String plainText = deviceId + ":" + timestamp + ":" + vinId;
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
