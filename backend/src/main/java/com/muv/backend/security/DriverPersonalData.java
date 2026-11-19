package com.muv.backend.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Reutiliza o formato AES-GCM e o índice HMAC já adotados no cadastro de contas. */
@Component
@Lazy
public class DriverPersonalData {
    private final PassengerPrivacy privacy;
    private final SecretKeySpec key;

    public DriverPersonalData(@Value("${muv.driver.personal-key:${MUV_DRIVER_PERSONAL_KEY:${MUV_AUTH_PASSENGER_KEY:}}}") String configuredKey) {
        byte[] bytes = Base64.getDecoder().decode(configuredKey);
        if (bytes.length != 64) throw new IllegalStateException("Configure MUV_DRIVER_PERSONAL_KEY com 64 bytes em Base64.");
        privacy = new PassengerPrivacy(configuredKey);
        key = new SecretKeySpec(bytes, 0, 32, "AES");
    }
    public String cpfIndex(String cpf) { return privacy.cpfIndex(cpf); }
    public String encrypt(String data, long accountId) { return privacy.encrypt(data, accountId); }
    public String decrypt(String data, long accountId) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, bytes, 0, 12));
            cipher.updateAAD(ByteBuffer.allocate(Long.BYTES).putLong(accountId).array());
            return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalStateException("Não foi possível ler os dados do motorista.", error);
        }
    }
}
