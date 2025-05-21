package io.quarkus.hibernate.validator.runtime;

import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.constraintvalidation.spi.DefaultConstraintValidatorFactory;

import io.smallrye.config.validator.BeanValidationConfigValidator;

public class HibernateBeanValidationConfigValidator implements BeanValidationConfigValidator {

    public HibernateBeanValidationConfigValidator(Set<String> constraints, Set<Class<?>> classesToBeValidated) {
        PredefinedScopeHibernateValidatorConfiguration configuration = Validation
                .byProvider(PredefinedScopeHibernateValidator.class)
                .configure();

        // TODO - There is no way to retrieve locales from configuration here (even manually). We need to add a way to configure the validator from SmallRye Config.
        configuration
                .ignoreXmlConfiguration()
                .builtinConstraints(constraints)
                .initializeBeanMetaData(classesToBeValidated)
                .constraintValidatorFactory(new DefaultConstraintValidatorFactory())
                .traversableResolver(new TraverseAllTraversableResolver());

        ConfigValidatorHolder.initialize(configuration.buildValidatorFactory());
    }

    @Override
    public Validator getValidator() {
        return ConfigValidatorHolder.getValidator();
    }

    // Store in a holder, so we can easily reference it and shutdown the validator
    public static class ConfigValidatorHolder {
        private static ValidatorFactory validatorFactory;
        private static Validator validator;

        static void initialize(ValidatorFactory validatorFactory) {
            ConfigValidatorHolder.validatorFactory = validatorFactory;
            ConfigValidatorHolder.validator = validatorFactory.getValidator();
        }

        static ValidatorFactory getValidatorFactory() {
            return validatorFactory;
        }

        static Validator getValidator() {
            return validator;
        }
    }
}
