package com.rea.sae_backend.security;

import java.text.ParseException;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtResourceServerConfig {

    /**
     * Decodificador dual durante la transición:
     * - Token OIDC de Keycloak (RS256 vía JWKS remoto, con validación de issuer).
     * - Token JWT local legacy (HS256 con el secreto de {@code jwt.secret}).
     * Cuando se elimine el login legacy (Fase 6), basta con devolver solo el
     * decodificador de Keycloak.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.keycloak.jwk-set-uri}") String jwkSetUri,
            @Value("${app.keycloak.issuer-uri}") String issuerUri,
            @Value("${jwt.secret}") String legacySecret) {

        NimbusJwtDecoder keycloakDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        keycloakDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));

        byte[] secretBytes = Base64.getDecoder().decode(legacySecret);
        SecretKey secretKey = Keys.hmacShaKeyFor(secretBytes);
        NimbusJwtDecoder legacyDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        return token -> {
            if (looksLikeLegacyHmacToken(token)) {
                try {
                    return legacyDecoder.decode(token);
                } catch (JwtException ex) {
                    return keycloakDecoder.decode(token);
                }
            }
            try {
                return keycloakDecoder.decode(token);
            } catch (JwtException ex) {
                return legacyDecoder.decode(token);
            }
        };
    }

    private boolean looksLikeLegacyHmacToken(String token) {
        try {
            JWSAlgorithm algorithm = SignedJWT.parse(token).getHeader().getAlgorithm();
            return algorithm != null && "HS256".equals(algorithm.getName());
        } catch (ParseException | RuntimeException ex) {
            return false;
        }
    }
}