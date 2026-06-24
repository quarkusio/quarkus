package io.quarkus.it.hibernate.validator.programmatic;

import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.defs.NotBlankDef;

import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import io.quarkus.it.hibernate.validator.programmatic.MyProgrammaticBean.NestedBean;

@ApplicationScoped
public class MyProgrammaticBeanValidatorCustomizer implements
        ValidatorFactoryCustomizer {

    @Override
    public void customize(BaseHibernateValidatorConfiguration<?> configuration) {
        ConstraintMapping constraintMapping = configuration.createConstraintMapping();
        constraintMapping
                .type(MyProgrammaticBean.class)
                .field("string").constraint(new NotBlankDef())
                .field("nestedBean").valid()
                .type(NestedBean.class)
                .field("string")
                .constraint(new NotBlankDef());
        configuration.addMapping(constraintMapping);
    }
}
