package io.quarkus.runtime;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocPrefix;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Localization.
 */
@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigDocPrefix("quarkus.locales")
public class LocalesBuildTimeConfig {

    // We set to en as the default language when all else fails since this is what the JDK does as well
    public static final String DEFAULT_LANGUAGE = "${user.language:en}";
    public static final String DEFAULT_COUNTRY = "${user.country:}";

    /**
     * The set of supported locales that can be consumed by the extensions.
     * <p>
     * The locales must be specified in the IETF BCP 47 format e.g. en-US or fr-FR.
     * <p>
     * For instance, the Hibernate Validator extension makes use of it.
     * <p>
     * Native-image build uses it to define additional locales that are supposed
     * to be available at runtime.
     * <p>
     * A special string "all" is translated as ROOT Locale and then used in native-image
     * to include all locales. Image size penalty applies.
     */
    @ConfigItem(defaultValue = DEFAULT_LANGUAGE + "-"
            + DEFAULT_COUNTRY, defaultValueDocumentation = "Set containing the build system locale")
    public Set<Locale> locales;

    /**
     * Default locale that can be consumed by the extensions.
     * <p>
     * The locale must be specified in the IETF BCP 47 format e.g. en-US or fr-FR.
     * <p>
     * For instance, the Hibernate Validator extension makes use of it.
     * <p>
     * Native-image build uses this property to derive {@code user.language} and {@code user.country} for the application's
     * runtime. Starting with GraalVM for JDK 24 {@code user.language} and {@code user.country} can also be overridden at
     * runtime, provided the selected locale was included at image build time.
     */
    @ConfigItem(defaultValueDocumentation = "Defaults to the JVM's default locale if not set. "
            + "Starting with GraalVM for JDK 24, it defaults to en-US for native executables.")
    public Optional<Locale> defaultLocale;
}
