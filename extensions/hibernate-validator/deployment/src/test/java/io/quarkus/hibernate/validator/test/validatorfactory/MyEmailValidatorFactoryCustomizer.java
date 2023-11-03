package io.quarkus.hibernate.validator.test.validatorfactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.Email;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;

@ApplicationScoped
public class MyEmailValidatorFactoryCustomizer implements ValidatorFactoryCustomizer {

    @Override
    public void customize(BaseHibernateValidatorConfiguration<?> configuration) {
        ConstraintMapping constraintMapping = configuration.createConstraintMapping();

        constraintMapping
                .constraintDefinition(Email.class)
                .includeExistingValidators(false)
                .validatedBy(MyEmailValidator.class);

        configuration.addMapping(constraintMapping);
    }
}
