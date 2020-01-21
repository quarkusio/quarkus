package io.quarkus.runtime;

import java.util.Locale;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LocalesBuildTimeConfig {

    // we set to en as the default language when all else fails since this is what the JDK does as well
    private static final String DEFAULT_LOCALE_VALUE = "${user.language:en}-${user.country:}";

    /**
     * The set of supported locales that can be consumed by the extensions.
     * <p>
     * The locales must be specified in the IETF BCP 47 format e.g. en-US or fr-FR.
     * <p>
     * For instance, the Hibernate Validator extension makes use of it.
     */
    @ConfigItem(defaultValue = DEFAULT_LOCALE_VALUE, defaultValueDocumentation = "Set containing the build system locale")
    public Set<Locale> locales;

    /**
     * Default locale that can be consumed by the extensions.
     * <p>
     * The locales must be specified in the IETF BCP 47 format e.g. en-US or fr-FR.
     * <p>
     * For instance, the Hibernate Validator extension makes use of it.
     */
    @ConfigItem(defaultValue = DEFAULT_LOCALE_VALUE, defaultValueDocumentation = "Build system locale")
    public Locale defaultLocale;
}
