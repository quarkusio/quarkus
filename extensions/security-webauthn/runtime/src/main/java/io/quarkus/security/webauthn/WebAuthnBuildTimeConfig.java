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
     * By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`,
     * so the endpoints are under `/q/webauthn`. An absolute path, such as `/auth/webauthn`, is used as is.
     *
     * The bundled `webauthn.js` keeps its default `/q/webauthn` endpoints, so when you change this path, pass the new
     * endpoints to the `WebAuthn` JavaScript constructor.
     */
    @WithDefault("webauthn")
    String rootPath();
}
