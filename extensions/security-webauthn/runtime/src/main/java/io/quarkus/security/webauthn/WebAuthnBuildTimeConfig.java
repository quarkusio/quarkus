package io.quarkus.security.webauthn;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.webauthn")
public interface WebAuthnBuildTimeConfig {

    /**
     * If the WebAuthn extension is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
