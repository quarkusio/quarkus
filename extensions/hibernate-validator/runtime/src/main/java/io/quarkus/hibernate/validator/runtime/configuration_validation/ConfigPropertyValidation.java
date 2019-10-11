package io.quarkus.hibernate.validator.runtime.configuration_validation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.validation.Validator;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ConfigPropertyValidation {

    @Inject
    Validator validator;

    /**
     * Validate all class with a configuration property during application startup.
     * This will halt application startup if a field is invalid
     */
    public void onStartUp(@Observes StartupEvent event) {
        for (ClassWithConfigProperties classWithConfigurationProperties : ClassWithConfigProperties.INSTANCES) {
            classWithConfigurationProperties.validate(validator);
        }
    }
}
