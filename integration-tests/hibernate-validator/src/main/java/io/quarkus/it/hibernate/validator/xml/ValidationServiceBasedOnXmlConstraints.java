package io.quarkus.it.hibernate.validator.xml;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@ApplicationScoped
public class ValidationServiceBasedOnXmlConstraints {

    @Inject
    Validator validator;

    public Set<ConstraintViolation<MyXmlBean>> validateSomeMyXmlBean() {
        return validator.validate(new MyXmlBean());
    }
}
