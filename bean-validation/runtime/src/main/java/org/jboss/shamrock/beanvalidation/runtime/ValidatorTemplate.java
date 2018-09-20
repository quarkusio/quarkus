package org.jboss.shamrock.beanvalidation.runtime;

import java.lang.reflect.Proxy;

import javax.validation.Validator;

import org.jboss.shamrock.runtime.InjectionInstance;

public class ValidatorTemplate {

    /**
     * Force the validation factory to be created at static init time, so it is
     * bootstrapped in a JVM rather than in native-image
     * <p>
     * TODO: we really only need to run in native-image
     *
     * @param provider
     */
    public void forceInit(InjectionInstance<ValidatorProvider> provider, Class<?>... classesToValidate) {
        provider.newInstance().forceInit();
        Validator validator = provider.newInstance().factory().getValidator();
        for(Class<?> i : classesToValidate) {
            validator.getConstraintsForClass(i);
        }
    }
}
