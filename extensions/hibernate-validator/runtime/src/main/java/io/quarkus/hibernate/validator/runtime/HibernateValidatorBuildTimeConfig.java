package io.quarkus.hibernate.validator.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configures Hibernate Validator properties.
 */

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateValidatorBuildTimeConfig {

    /**
     * Configures validation error message locales.
     */
    @ConfigItem(defaultValue = "${user.language}-${user.country}")
    public List<String> locales;

    /**
     * Configures validation default locale.
     */
    @ConfigItem(defaultValue = "${user.language}-${user.country}")
    public String defaultLocale;
}
