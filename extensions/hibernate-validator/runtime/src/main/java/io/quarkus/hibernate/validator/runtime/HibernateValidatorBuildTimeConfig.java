package io.quarkus.hibernate.validator.runtime;

import java.util.Locale;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateValidatorBuildTimeConfig {

    /**
     * The locales supported by Hibernate Validator for message interpolation.
     */
    @ConfigItem(defaultValue = "${user.language}-${user.country}")
    public Set<Locale> locales;

    /**
     * The default locale used for message interpolation.
     */
    @ConfigItem(defaultValue = "${user.language}-${user.country}")
    public Locale defaultLocale;
}
