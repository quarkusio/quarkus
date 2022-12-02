package io.quarkus.hibernate.validator.test.validatorfactory;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

import io.quarkus.hibernate.validator.HibernateValidatorFactoryCustomizer;

@ApplicationScoped
public class MyMultipleHibernateValidatorFactoryCustomizer implements HibernateValidatorFactoryCustomizer {

    @Override
    public void customize(BaseHibernateValidatorConfiguration<?> configuration) {
        ConstraintMapping constraintMapping = configuration.createConstraintMapping();

        constraintMapping
                .constraintDefinition(Email.class)
                .includeExistingValidators(false)
                .validatedBy(MyEmailValidator.class);

        configuration.addMapping(constraintMapping);

        constraintMapping = configuration.createConstraintMapping();

        constraintMapping
                .constraintDefinition(Min.class)
                .includeExistingValidators(false)
                .validatedBy(MyNumValidator.class);

        configuration.addMapping(constraintMapping);
    }
}
