package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect de débogage WebAuthn : intercepte toutes les opérations WebAuthn
 * et logue les erreurs (notamment BadOriginException) avec les origines
 * configurées pour faciliter le diagnostic.
 *
 * <p>Actif dans tous les profils (local, prod, etc.).
 */
@Aspect
@Component
public class WebAuthnDebugAspect {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnDebugAspect.class);

    @Value("${app.webauthn.rp-id:localhost}")
    private String rpId;

    @Value("${app.webauthn.allowed-origins:http://localhost:8080}")
    private String allowedOriginsRaw;

    @PostConstruct
    public void init() {
        String msg = "[WebAuthn DEBUG] Aspect actif — RP-ID: " + rpId
                + " | Origines autorisées: " + allowedOriginsRaw;
        System.err.println(msg);
        log.error(msg);
    }

    /**
     * Intercepte uniquement les appels sur l'interface RelyingPartyOperations
     * (registration + authentication). Utiliser l'interface évite que CGLIB
     * tente de proxifier les classes final/package-private du package WebAuthn.
     */
    @Around("execution(* org.springframework.security.web.webauthn.management.RelyingPartyOperations.*(..))")
    public Object logWebAuthnOps(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            String[] allowed = Arrays.stream(allowedOriginsRaw.split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
            String fullMsg = buildMessage(ex);
            boolean isBadOrigin = fullMsg.toLowerCase().contains("origin")
                    || fullMsg.toLowerCase().contains("badorigin");

            String logLine = "[WebAuthn DEBUG] Exception sur "
                    + pjp.getTarget().getClass().getSimpleName() + "#" + pjp.getSignature().getName()
                    + "\n  RP-ID configuré     : " + rpId
                    + "\n  Origines autorisées : " + Arrays.toString(allowed)
                    + "\n  Exception           : " + fullMsg
                    + (isBadOrigin ? "\n  → BadOriginException détectée : vérifiez scheme+host+port" : "");

            // Double sortie : stderr (toujours visible) + logger ERROR
            System.err.println(logLine);
            log.error(logLine);

            throw ex;
        }
    }

    /**
     * Construit un message lisible en remontant la chaîne de causes.
     */
    private String buildMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable current = ex;
        for (int depth = 0; depth < 5 && current != null; depth++) {
            if (depth > 0) sb.append(" → caused by: ");
            sb.append(current.getClass().getSimpleName())
              .append(": ")
              .append(current.getMessage());
            current = current.getCause();
        }
        return sb.toString();
    }
}
