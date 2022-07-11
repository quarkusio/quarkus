package io.quarkus.hibernate.validator.test.validatorfactory;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.Email;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

import io.quarkus.hibernate.validator.HibernateValidatorFactoryCustomizer;

@ApplicationScoped
public class MyEmailHibernateValidatorFactoryCustomizer implements HibernateValidatorFactoryCustomizer {

    @Override
    public <T extends BaseHibernateValidatorConfiguration<T>> void customize(T configuration) {
        ConstraintMapping constraintMapping = configuration.createConstraintMapping();

        constraintMapping
                .constraintDefinition(Email.class)
                .includeExistingValidators(false)
                .validatedBy(MyEmailValidator.class);

        configuration.addMapping(constraintMapping);
    }
}
