package com.sleekydz86.catalog.domain.connection.port;

public interface SecretCipherPort {
    String encrypt(String rawSecret);
    String decrypt(String encryptedSecret);
}
