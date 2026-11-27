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

    /**
     * The path under which the WebAuthn endpoints are registered.
     *
     * This value is resolved as a path relative to `${quarkus.http.root-path}`, so a leading slash makes no difference.
     * These are application paths, so they do not follow `${quarkus.http.non-application-root-path}`.
     *
     * The bundled `webauthn.js` keeps its default `/q/webauthn` endpoints, so when you change this path, pass the new
     * endpoints to the `WebAuthn` JavaScript constructor.
     */
    @WithDefault("q/webauthn")
    String rootPath();
}
