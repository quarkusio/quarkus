package org.jboss.shamrock.beanvalidation.runtime;

import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

@ApplicationScoped
public class ValidatorProvider {

    ValidatorFactory factory;

    @PostConstruct
    public void setup() {
        Configuration<?> configure = Validation.byDefaultProvider().configure();
        try {
            Class<?> cl = Class.forName("javax.el.ELManager");
            Method method = cl.getDeclaredMethod("getExpressionFactory");
            method.invoke(null);
        } catch (Throwable t) {
            //if EL is not on the class path we use the parameter message interpolator
            configure.messageInterpolator(new ParameterMessageInterpolator());
        }
        factory = configure.buildValidatorFactory();
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
