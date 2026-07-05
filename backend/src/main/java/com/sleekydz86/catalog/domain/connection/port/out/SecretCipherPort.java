package com.sleekydz86.catalog.domain.connection.port.out;

public interface SecretCipherPort {
    String encrypt(String rawSecret);
    String decrypt(String encryptedSecret);
}
