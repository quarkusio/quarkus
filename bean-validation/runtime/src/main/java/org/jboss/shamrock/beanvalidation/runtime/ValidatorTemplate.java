package org.jboss.shamrock.beanvalidation.runtime;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.InjectionInstance;

@Template
public class ValidatorTemplate {

    /**
     * Force the validation factory to be created at static init time, so it is
     * bootstrapped in a JVM rather than in native-image
     * <p>
     * TODO: we really only need to run in native-image
     *
     * @param provider
     */
    public void forceInit(InjectionFactory provider, Class<?>... classesToValidate) {
        ValidatorProvider validatorProvider = provider.create(ValidatorProvider.class).newInstance();
        validatorProvider.forceInit();
        Validator validator = validatorProvider.factory().getValidator();
        for (Class<?> i : classesToValidate) {
            validator.getConstraintsForClass(i);
        }
    }
}
