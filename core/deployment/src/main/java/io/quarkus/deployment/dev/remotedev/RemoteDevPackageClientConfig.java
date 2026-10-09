package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Optional;

public record RemoteDevPackageClientConfig(
        URI remoteUrl,
        Optional<String> password) {

    public RemoteDevPackageClientConfig {
        requireNonNull(remoteUrl, "remoteUrl");
        requireNonNull(password, "password");
    }

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
