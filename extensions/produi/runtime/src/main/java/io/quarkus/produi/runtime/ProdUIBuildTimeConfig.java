package io.quarkus.produi.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.prod-ui")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ProdUIBuildTimeConfig {

    /**
     * Enable Prod UI. When enabled, selected extension pages and JsonRPC methods
     * are available in production via the management interface.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Serve Prod UI on the management interface when the management interface is enabled.
     */
    @WithDefault("true")
    boolean managementEnabled();

    /**
     * URL path for Prod UI under the non-application root path.
     */
    @WithDefault("prod-ui")
    String path();

    /**
     * Roles allowed to access Prod UI.
     * <p>
     * When set, every Prod UI route - including the JSON-RPC websocket that carries all data - requires an authenticated
     * user with at least one of these roles. Authentication itself is delegated to the management interface
     * (configure it with {@code quarkus.management.auth.*}, e.g. basic, OIDC or mTLS). When empty (the default), Prod UI
     * adds no authorization of its own and relies on however the management interface is secured.
     */
    Optional<List<String>> rolesAllowed();

    /**
     * Exclude Prod UI's own artifacts from its views.
     * <p>
     * When {@code true} (the default), Prod UI hides its own loggers ({@code io.quarkus.produi.*}) and its own
     * dependencies ({@code io.quarkus:quarkus-produi*}) so they do not add noise to the Loggers and Dependencies pages.
     * Prod UI's own configuration ({@code quarkus.prod-ui.*}) is deliberately left visible on the Configuration page so
     * operators can still verify how Prod UI itself is set up.
     */
    @WithDefault("true")
    boolean excludeSelf();

    /**
     * Per-page configuration, keyed by page id.
     * <p>
     * The built-in pages have the ids {@code advisor}, {@code configuration}, {@code endpoints}, {@code loggers} and
     * {@code dependencies}; an extension-contributed page is keyed by its extension name (e.g. {@code quarkus-cache}).
     * Use this to hide individual pages in production, for example
     * {@code quarkus.prod-ui.pages.configuration.enabled=false}.
     */
    @ConfigDocMapKey("page-id")
    Map<String, PageConfig> pages();

    interface PageConfig {
        /**
         * Whether this page is shown. Pages are shown by default.
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Gated diagnostic actions.
     * <p>
     * Prod UI is strictly read-only by default; every diagnostic here is disabled by default and must be explicitly
     * opted into by an operator. Even when enabled, these actions are still gated by whatever secures Prod UI (see
     * {@link #rolesAllowed()} and the management interface auth), the UI asks for confirmation, and each invocation is
     * audit-logged.
     */
    Diagnostics diagnostics();

    interface Diagnostics {
        /**
         * Whether the gated thread dump action is available.
         * <p>
         * When {@code true}, an authorized operator can capture a read-only thread dump (stack traces only - no heap
         * dump, no file written, and no variable/field values, so no secrets are exposed). Disabled by default so the
         * default Prod UI stays strictly read-only. Each captured dump is audit-logged.
         */
        @WithDefault("false")
        boolean threadDump();
    }
}
