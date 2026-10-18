package io.quarkus.hibernate.validator;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for the default
 * {@link jakarta.validation.ValidatorFactory}.
 * <p>
 * All implementations that are registered as CDI beans are taken into account when producing the default
 * {@link jakarta.validation.ValidatorFactory}.
 * <p>
 * Customizers are applied in the order of {@link jakarta.annotation.Priority}.
 *
 * @deprecated Implement {@link ValidatorFactoryCustomizer} instead.
 */
@Deprecated(forRemoval = true)
public interface HibernateValidatorFactoryCustomizer extends ValidatorFactoryCustomizer {

    void customize(BaseHibernateValidatorConfiguration<?> configuration);
}
