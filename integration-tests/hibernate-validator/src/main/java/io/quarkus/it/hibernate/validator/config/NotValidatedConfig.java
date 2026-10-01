package io.quarkus.it.hibernate.validator.config;

import io.smallrye.config.ConfigMapping;

/**
 * A config mapping with no Bean Validation constraints anywhere in its tree. Together with
 * {@link RootValidatedConfig} and {@link NestedValidatedConfig}, this covers a native-mode
 * regression where the validator used to reflectively touch every config mapping once Bean
 * Validation was active anywhere, even mappings with no constraints of their own.
 */
@ConfigMapping(prefix = "config-validation.not-validated")
public interface NotValidatedConfig {

    String value();
}
