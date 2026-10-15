package io.quarkus.hibernate.validator.runtime;

import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.constraintvalidation.spi.DefaultConstraintValidatorFactory;

import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.validator.BeanValidationConfigValidator;

public class HibernateBeanValidationConfigValidator implements BeanValidationConfigValidator {

    private final Set<Class<?>> mappingsRequiringValidation;

    public HibernateBeanValidationConfigValidator(Set<String> constraints, Set<Class<?>> classesToBeValidated,
            Set<Class<?>> mappingsRequiringValidation) {
        this.mappingsRequiringValidation = mappingsRequiringValidation;

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
    public void validateMapping(GeneratedConfigClass configClass, Object configObject) throws ConfigValidationException {
        // Every config mapping in the application is validated through this single validator, even mappings
        // that have no Bean Validation constraints anywhere in their tree. Skip those entirely: their generated
        // mapping interface was never registered for reflection, since only constrained trees are (see
        // HibernateValidatorProcessor#configValidator), so walking into them would blow up in native mode.
        if (!mappingsRequiringValidation.contains(configClass.getParent())) {
            return;
        }
        BeanValidationConfigValidator.super.validateMapping(configClass, configObject);
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
