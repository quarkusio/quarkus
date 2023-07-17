package io.quarkus.hibernate.validator.test.validatorfactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.Min;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;

@ApplicationScoped
public class MyNumberValidatorFactoryCustomizer implements ValidatorFactoryCustomizer {

    @Override
    public void customize(BaseHibernateValidatorConfiguration<?> configuration) {
        ConstraintMapping constraintMapping = configuration.createConstraintMapping();

        constraintMapping
                .constraintDefinition(Min.class)
                .includeExistingValidators(false)
                .validatedBy(MyNumValidator.class);

        configuration.addMapping(constraintMapping);
    }
}
