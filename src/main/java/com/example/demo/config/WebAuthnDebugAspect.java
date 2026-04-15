package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Affiche au démarrage la configuration WebAuthn (RP-ID et origines autorisées)
 * pour faciliter le diagnostic des erreurs BadOriginException.
 */
@Component
public class WebAuthnDebugAspect {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnDebugAspect.class);

    @Value("${app.webauthn.rp-id:localhost}")
    private String rpId;

    @Value("${app.webauthn.allowed-origins:http://localhost:8080}")
    private String allowedOriginsRaw;

    @PostConstruct
    public void logWebAuthnConfig() {
        String[] origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        log.warn("========== WebAuthn Configuration ==========");
        log.warn("  RP-ID               : {}", rpId);
        log.warn("  Origines autorisées : {}", Arrays.toString(origins));
        log.warn("  → L'origine du navigateur doit correspondre exactement");
        log.warn("    (scheme + host + port, ex: https://mondomaine.com)");
        log.warn("============================================");
    }
}
