package com.petproject.integration.rest.security;

import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Генерує RSA keypair при старті застосунку — приватним ключем підписуються
 * JWT (AuthTokenController), публічний публікується як JWKS (JwkSetController),
 * і саме на нього спирається Resource Server (SecurityConfig) під час перевірки
 * підпису вхідних токенів. У реальній системі це робив би зовнішній Authorization
 * Server (Keycloak / Azure AD / SAP BTP XSUAA) — тут усе відбувається в одному
 * процесі виключно для навчальної демонстрації механізму OAuth2/JWT.
 */
@Component
public class RsaKeyProvider {

    public static final String KEY_ID = "demo-key";

    private final KeyPair keyPair;

    public RsaKeyProvider() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate RSA keypair for demo JWT issuer", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    public RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }
}
