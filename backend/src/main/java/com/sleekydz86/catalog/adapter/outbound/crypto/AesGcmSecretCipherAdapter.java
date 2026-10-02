package com.sleekydz86.catalog.adapter.outbound.crypto;

import com.sleekydz86.catalog.domain.connection.port.out.SecretCipherPort;
import com.sleekydz86.catalog.global.config.ConnectionModuleProperties;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.sleekydz86.catalog.global.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesGcmSecretCipherAdapter implements SecretCipherPort {

    private static final Logger log = LoggerFactory.getLogger(AesGcmSecretCipherAdapter.class);
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final ConnectionModuleProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSecretCipherAdapter(ConnectionModuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public String encrypt(String rawSecret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(normalize(rawSecret).getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            log.error("연결 비밀번호 암호화 실패", exception);
            throw InfrastructureException.of(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "연결 비밀번호 암호화에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public String decrypt(String encryptedSecret) {
        try {
            byte[] payload = Base64.getDecoder().decode(normalize(encryptedSecret));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("암호화된 비밀번호 형식이 올바르지 않습니다.");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            log.error("연결 비밀번호 복호화 실패", exception);
            throw InfrastructureException.of(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "연결 비밀번호 복호화에 실패했습니다.",
                    exception
            );
        }
    }

    private SecretKeySpec secretKey() {
        String masterKey = properties.secretMasterKey();
        if (masterKey == null || masterKey.isBlank()) {
            throw InfrastructureException.of(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "연결 비밀번호 암호화 키가 설정되지 않았습니다.",
                    null
            );
        }
        try {
            byte[] key = Base64.getDecoder().decode(masterKey);
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw InfrastructureException.of(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "연결 비밀번호 암호화 키 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }
}
