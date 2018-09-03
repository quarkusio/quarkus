package org.jboss.shamrock.beanvalidation.runtime;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@ApplicationScoped
public class ValidatorProvider {

    ValidatorFactory factory;

    @PostConstruct
    public void setup() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @Produces
    public ValidatorFactory factory() {
        return factory;
    }

    @Produces
    public Validator validator() {
        return factory.getValidator();
    }

    public void forceInit() {

    }

}
