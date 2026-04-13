package com.example.demo.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect de débogage WebAuthn : lorsqu'une BadOriginException est levée lors
 * de la vérification d'une passkey, logue les origines autorisées configurées
 * ainsi que l'origine reçue pour faciliter le diagnostic.
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

    /**
     * Intercepte tous les appels sur RelyingPartyOperations (registration + authentication).
     * En cas d'exception de type BadOriginException (Spring Security ou webauthn4j),
     * log un message d'erreur enrichi avant de relancer l'exception.
     */
    @Around("execution(* org.springframework.security.web.webauthn.management.RelyingPartyOperations.*(..))")
    public Object logBadOrigin(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            if (isBadOriginException(ex)) {
                String[] allowed = Arrays.stream(allowedOriginsRaw.split(","))
                        .map(String::trim)
                        .toArray(String[]::new);
                log.error("""
                        [WebAuthn DEBUG] BadOriginException interceptée sur {}#{}
                          RP-ID configuré       : {}
                          Origines autorisées   : {}
                          Message de l'exception: {}
                        → Vérifiez que WEBAUTHN_ALLOWED_ORIGINS correspond exactement à \
                        l'origine du navigateur (scheme + host + port).
                        """,
                        pjp.getTarget().getClass().getSimpleName(),
                        pjp.getSignature().getName(),
                        rpId,
                        Arrays.toString(allowed),
                        buildMessage(ex));
            }
            throw ex;
        }
    }

    /**
     * Vérifie si l'exception (ou l'une de ses causes) est une BadOriginException,
     * qu'elle vienne de Spring Security ou de webauthn4j.
     */
    private boolean isBadOriginException(Throwable ex) {
        Throwable current = ex;
        for (int depth = 0; depth < 10 && current != null; depth++) {
            String name = current.getClass().getName();
            if (name.contains("BadOriginException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
