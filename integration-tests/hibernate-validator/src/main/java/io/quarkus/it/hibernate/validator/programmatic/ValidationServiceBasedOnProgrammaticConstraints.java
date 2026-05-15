package io.quarkus.it.hibernate.validator.programmatic;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import io.quarkus.it.hibernate.validator.programmatic.MyProgrammaticBean.NestedBean;

@ApplicationScoped
public class ValidationServiceBasedOnProgrammaticConstraints {

    @Inject
    Validator validator;

    public Set<ConstraintViolation<MyProgrammaticBean>> validateSomeMyProgrammaticBean() {
        return validator.validate(new MyProgrammaticBean("", new NestedBean("")));
    }
}
