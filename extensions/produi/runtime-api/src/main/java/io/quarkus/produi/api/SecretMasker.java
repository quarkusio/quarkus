package io.quarkus.produi.api;

import java.util.List;
import java.util.Locale;

/**
 * Centralised secret masking for Prod UI pages.
 * <p>
 * Prod UI is served in production, so credential-bearing values must never leave the server. Any page that surfaces
 * configuration, connection strings or endpoints should route values through this class rather than re-implementing its
 * own heuristics. Masking is deliberately over-inclusive: hiding a non-secret is safer than leaking a secret.
 * <p>
 * This lives in the lightweight {@code quarkus-produi-api} module so per-extension Prod UI services can depend on it
 * without pulling in the full Prod UI runtime.
 */
public final class SecretMasker {

    /** The fixed replacement shown in place of a secret value. Fixed length so it never reveals the real length. */
    public static final String MASK = "**********";

    // Property-name fragments that indicate a credential-bearing value (matched case-insensitively).
    private static final List<String> SECRET_NAME_FRAGMENTS = List.of(
            "password", "passwd", "secret", "credential", "token",
            "passphrase", "private-key", "privatekey", "secret-key", "secretkey",
            "api-key", "apikey", "access-key", "accesskey");

    private SecretMasker() {
    }

    /**
     * @return {@code true} if the property name looks credential-bearing.
     */
    public static boolean isSecretName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String fragment : SECRET_NAME_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the value carries an inline HTTP authorization (e.g. an OTLP exporter
     *         {@code Authorization=Bearer ...} header carried as a single config value).
     */
    public static boolean isSecretValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("authorization=") || lower.contains("bearer ") || lower.contains("basic ");
    }

    /**
     * @return {@code true} if either the name or the value indicates a secret.
     */
    public static boolean isSecret(String name, String value) {
        return isSecretName(name) || isSecretValue(value);
    }

    /**
     * Masks {@code value} when {@code (name, value)} is a non-empty secret, otherwise returns it unchanged.
     * A {@code null} value is normalised to an empty string.
     */
    public static String maskIfSecret(String name, String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        return isSecret(name, value) ? MASK : value;
    }

    /**
     * Strips the {@code user:password@} user-info from a URL so credentials embedded in a connection string or endpoint
     * are never shown (e.g. {@code mongodb://user:pass@host} becomes {@code mongodb://**********@host}). Values that are
     * not URLs, or whose {@code @} belongs to the path rather than the authority, are returned unchanged.
     */
    public static String maskUrlCredentials(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        int authorityStart = scheme + 3;
        int at = url.indexOf('@', authorityStart);
        if (at < 0) {
            return url;
        }
        // Only treat the '@' as user-info if it comes before the path/query, i.e. it is part of the authority.
        int firstSlash = url.indexOf('/', authorityStart);
        if (firstSlash >= 0 && at > firstSlash) {
            return url;
        }
        return url.substring(0, authorityStart) + MASK + url.substring(at);
    }
}
