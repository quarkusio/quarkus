package io.quarkus.devui.deployment;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.dev-ui")
public interface DevUIConfig {

    /**
     * The number of history log entries to remember.
     */
    @WithDefault("50")
    int historySize();

    /**
     * Show the JsonRPC Log. Useful for extension developers
     */
    @WithDefault("false")
    boolean showJsonRpcLog();

    /**
     * Set the base theme.
     *
     * @return Theme
     */
    @WithDefault("quarkus")
    BaseTheme baseTheme();

    /**
     * More hosts allowed for Dev UI
     *
     * Comma separated list of valid URLs, e.g.: www.quarkus.io, myhost.com
     * (This can also be a regex)
     * By default localhost and 127.0.0.1 will always be allowed
     */
    Optional<List<String>> hosts();

    /**
     * Set a context root for dev-ui. This is useful for remote environments or online IDEs
     *
     * @return The dev-ui context root
     */
    Optional<String> contextRoot();

    /**
     * Workspace configuration.
     */
    Workspace workspace();

    /**
     * CORS configuration.
     */
    Cors cors();

    /**
     * Enable/Disable the ability to add and remove extensions from Dev UI
     */
    @WithDefault("true")
    boolean allowExtensionManagement();

    /**
     * Fine tune the theme
     */
    Optional<Theme> theme();

    @ConfigGroup
    interface Workspace {

        /**
         * Folders to ignore in the workspace
         */
        Optional<List<String>> ignoreFolders();

        /**
         * Files to ignore in the workspace
         */
        Optional<List<Pattern>> ignoreFiles();

    }

    @ConfigGroup
    interface Cors {

        /**
         * Enable CORS filter.
         */
        @WithDefault("true")
        boolean enabled();
    }

    @ConfigGroup
    interface Theme {
        /**
         * Override the dark theme vars
         */
        Optional<ThemeMode> dark();

        /**
         * Override the light theme vars
         */
        Optional<ThemeMode> light();
    }

    @ConfigGroup
    interface ThemeMode {
        /**
         * This will replace the theme base-color
         */
        Optional<String> baseColor();

        /**
         * This will replace the theme contrast
         */
        Optional<String> contrast();

        /**
         * This will replace the theme primary-color
         */
        Optional<String> primaryColor();

        /**
         * This will replace the theme primary-text-color
         */
        Optional<String> primaryTextColor();

        /**
         * This will replace the theme primary-contrast-ccolor
         */
        Optional<String> primaryContrastColor();

        /**
         * This will replace the theme error-color
         */
        Optional<String> errorColor();

        /**
         * This will replace the theme error-text-color
         */
        Optional<String> errorTextColor();

        /**
         * This will replace the theme error-contrast-color
         */
        Optional<String> errorContrastColor();

        /**
         * This will replace the theme warning-color
         */
        Optional<String> warningColor();

        /**
         * This will replace the theme warning-text-color
         */
        Optional<String> warningTextColor();

        /**
         * This will replace the theme warning-contrast-color
         */
        Optional<String> warningContrastColor();

        /**
         * This will replace the theme success-color
         */
        Optional<String> successColor();

        /**
         * This will replace the theme success-text-color
         */
        Optional<String> successTextColor();

        /**
         * This will replace the theme success-contrast-color
         */
        Optional<String> successContrastColor();

        /**
         * This will replace the theme header-text-color
         */
        Optional<String> headerTextColor();

        /**
         * This will replace the theme body-text-color
         */
        Optional<String> bodyTextColor();

        /**
         * This will replace the theme secondary-text-color
         */
        Optional<String> secondaryTextColor();

        /**
         * This will replace the theme tertiary-text-color
         */
        Optional<String> tertiaryTextColor();

        /**
         * This will replace the theme disabled-text-color
         */
        Optional<String> disabledTextColor();

        /**
         * This will replace the theme contrast-5-pct
         */
        Optional<String> contrast5pct();

        /**
         * This will replace the theme contrast-10-pct
         */
        Optional<String> contrast10pct();

        /**
         * This will replace the theme contrast-15-pct
         */
        Optional<String> contrast15pct();

        /**
         * This will replace the theme contrast-20-pct
         */
        Optional<String> contrast20pct();

        /**
         * This will replace the theme contrast-25-pct
         */
        Optional<String> contrast25pct();

        /**
         * This will replace the theme contrast-30-pct
         */
        Optional<String> contrast30pct();

        /**
         * This will replace the theme contrast-35-pct
         */
        Optional<String> contrast35pct();

        /**
         * This will replace the theme contrast-40-pct
         */
        Optional<String> contrast40pct();

        /**
         * This will replace the theme contrast-45-pct
         */
        Optional<String> contrast45pct();

        /**
         * This will replace the theme contrast-50-pct
         */
        Optional<String> contrast50pct();

        /**
         * This will replace the theme contrast-55-pct
         */
        Optional<String> contrast55pct();

        /**
         * This will replace the theme contrast-60-pct
         */
        Optional<String> contrast60pct();

        /**
         * This will replace the theme contrast-65-pct
         */
        Optional<String> contrast65pct();

        /**
         * This will replace the theme contrast-70-pct
         */
        Optional<String> contrast70pct();

        /**
         * This will replace the theme contrast-75-pct
         */
        Optional<String> contrast75pct();

        /**
         * This will replace the theme contrast-80-pct
         */
        Optional<String> contrast80pct();

        /**
         * This will replace the theme contrast-85-pct
         */
        Optional<String> contrast85pct();

        /**
         * This will replace the theme contrast-90-pct
         */
        Optional<String> contrast90pct();
    }

    static enum BaseTheme {
        quarkus,
        red,
        blue
    }

}
