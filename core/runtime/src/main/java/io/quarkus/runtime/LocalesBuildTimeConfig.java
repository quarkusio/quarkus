package io.quarkus.runtime;

import java.util.Locale;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LocalesBuildTimeConfig {

    /**
     * The set of supported locales that can be consumed by the extensions.
     * <p>
     * The locales must be specified in the IETF BCP 47 format e.g. en-US or fr-FR.
     * <p>
     * For instance, the Hibernate Validator extension makes use of it.
     */
    @ConfigItem(defaultValue = "${user.language}-${user.country}", defaultValueDocumentation = "Set containing the build system locale")
    public Set<Locale> locales;

    /**
     * Default locale that can be consumed by the extensions.
     * <p>
     * The locales must be specified in the IETF BCP 47 format e.g. en-US or fr-FR.
     * <p>
     * For instance, the Hibernate Validator extension makes use of it.
     */
    @ConfigItem(defaultValue = "${user.language}-${user.country}", defaultValueDocumentation = "Build system locale")
    public Locale defaultLocale;
}
