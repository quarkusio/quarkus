package io.quarkus.hibernate.validator.runtime;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.validation.Validation;

import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import io.quarkus.runtime.annotations.Template;

@Template
public class HibernateValidatorTemplate {

    public void initializeValidatorFactory(Set<Class<?>> classesToBeValidated) {
        PredefinedScopeHibernateValidatorConfiguration configuration = Validation
                .byProvider(PredefinedScopeHibernateValidator.class)
                .configure();

        Set<Locale> localesToInitialize = Collections.singleton(Locale.getDefault());

        try {
            Class<?> cl = Class.forName("javax.el.ELManager");
            Method method = cl.getDeclaredMethod("getExpressionFactory");
            method.invoke(null);
        } catch (Throwable t) {
            //if EL is not on the class path we use the parameter message interpolator
            configuration.messageInterpolator(new ParameterMessageInterpolator(localesToInitialize));
        }

        configuration
                .initializeBeanMetaData(classesToBeValidated)
                .initializeLocales(localesToInitialize)
                .beanMetaDataClassNormalizer(new ArcProxyBeanMetaDataClassNormalizer());

        ValidatorHolder.initialize(configuration.buildValidatorFactory());
    }
}
