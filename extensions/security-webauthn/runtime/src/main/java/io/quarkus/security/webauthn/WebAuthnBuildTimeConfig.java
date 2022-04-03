package io.quarkus.security.webauthn;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "webauthn")
public class WebAuthnBuildTimeConfig {

    /**
     * If the WebAuthn extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
