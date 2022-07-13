package io.quarkus.hibernate.validator;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for the default
 * {@link javax.validation.ValidatorFactory}.
 * <p>
 * All implementations that are registered as CDI beans are taken into account when producing the default
 * {@link javax.validation.ValidatorFactory}.
 * <p>
 * Customizers are applied in the order of {@link javax.annotation.Priority}.
 */
public interface HibernateValidatorFactoryCustomizer {

    void customize(BaseHibernateValidatorConfiguration<?> configuration);
}
