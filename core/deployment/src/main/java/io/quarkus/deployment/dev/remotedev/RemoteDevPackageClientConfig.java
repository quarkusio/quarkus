package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Optional;

/**
 * Connection configuration for a {@link RemoteDevPackageClient}.
 * <p>
 * <strong>API note:</strong>
 * This immutable value is an internal Quarkus build-tool integration contract, not a user configuration object.
 * Build-tool plugins are responsible for mapping their user-facing configuration into it.
 *
 * @param remoteUrl non-{@code null} base URI of the Quarkus remote-development endpoint; this type does not provide a
 *        default or validate the URI scheme
 * @param password non-{@code null} optional remote-development password; an empty value is representable, although the
 *        built-in HTTP client requires a password for authenticated synchronization requests
 */
public record RemoteDevPackageClientConfig(
        URI remoteUrl,
        Optional<String> password) {

    /**
     * Validates that both record components are present.
     *
     * @throws NullPointerException if {@code remoteUrl} or {@code password} is {@code null}
     */
    public RemoteDevPackageClientConfig {
        requireNonNull(remoteUrl, "remoteUrl");
        requireNonNull(password, "password");
    }

    /**
     * Returns the configured URI with URI user-info, when present, replaced by a marker suitable for diagnostics.
     * <p>
     * This method does not inspect or redact arbitrary path or query values. Secrets must be supplied through
     * {@link #password()} rather than embedded in another URI component.
     *
     * @return a diagnostic representation of {@link #remoteUrl()} that does not expose URI user-info
     */
    public String redactedRemoteUrl() {
        String value = remoteUrl.toString();
        int scheme = value.indexOf("://");
        int at = value.indexOf('@', scheme + 3);
        if (scheme >= 0 && at > scheme) {
            return value.substring(0, scheme + 3) + "<redacted>@" + value.substring(at + 1);
        }
        return value;
    }
}
