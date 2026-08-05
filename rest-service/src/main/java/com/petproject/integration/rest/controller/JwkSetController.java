package com.petproject.integration.rest.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.petproject.integration.rest.security.RsaKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWK Set endpoint (RFC 7517) — саме сюди Spring Security (NimbusJwtDecoder)
 * ходить, щоб отримати публічний ключ і перевірити підпис вхідного JWT
 * (spring.security.oauth2.resourceserver.jwt.jwk-set-uri в application.yml).
 */
@RestController
public class JwkSetController {

    private final RsaKeyProvider rsaKeyProvider;

    public JwkSetController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyProvider.getPublicKey())
                .keyID(RsaKeyProvider.KEY_ID)
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}
