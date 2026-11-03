package io.quarkus.tls.runtime.letsencrypt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The HTTP-01 challenges that the Let's Encrypt client of the application answers. The challenge route of the
 * TLS registry serves them at {@code /.well-known/acme-challenge/<token>}.
 */
public final class LetsEncryptChallenges {

    private static final Map<String, String> KEY_AUTHORIZATIONS = new ConcurrentHashMap<>();

    private LetsEncryptChallenges() {
    }

    /**
     * Get the key authorization of a token.
     *
     * @param token the token of the challenge
     * @return the key authorization, or {@code null} if the client does not answer this token
     */
    public static String keyAuthorization(String token) {
        return KEY_AUTHORIZATIONS.get(token);
    }

    static void publish(String token, String keyAuthorization) {
        KEY_AUTHORIZATIONS.put(token, keyAuthorization);
    }

    static void remove(String token) {
        KEY_AUTHORIZATIONS.remove(token);
    }
}
