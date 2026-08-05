package com.petproject.integration.rest.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.petproject.integration.rest.security.RsaKeyProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * НАВЧАЛЬНИЙ mock Authorization Server у стилі OAuth2 Client Credentials grant
 * (RFC 6749 §4.4): видає підписаний JWT access token за client_id/scope без
 * реальної перевірки клієнта. У продакшн-інтеграції цю роль виконує окремий
 * Authorization Server (наприклад SAP BTP XSUAA, Keycloak, Azure AD) —
 * тут мета лише показати механіку видачі та подальшої перевірки JWT.
 */
@RestController
public class AuthTokenController {

    private static final String ISSUER = "https://petproject.integration/mock-authorization-server";

    private final RsaKeyProvider rsaKeyProvider;

    public AuthTokenController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @PostMapping("/api/public/auth/token")
    public Map<String, Object> issueToken(
            @RequestParam(defaultValue = "integration-client") String subject,
            @RequestParam(defaultValue = "orders.read orders.write") String scope) throws JOSEException {

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(subject)
                .claim("scope", scope)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(5))))
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RsaKeyProvider.KEY_ID).build(),
                claims);
        signedJwt.sign(new RSASSASigner(rsaKeyProvider.getPrivateKey()));

        return Map.of(
                "access_token", signedJwt.serialize(),
                "token_type", "Bearer",
                "expires_in", 300);
    }
}
