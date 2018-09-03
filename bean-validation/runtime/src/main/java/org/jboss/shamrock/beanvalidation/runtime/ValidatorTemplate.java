package org.jboss.shamrock.beanvalidation.runtime;

import org.jboss.shamrock.runtime.InjectionInstance;

public class ValidatorTemplate {

    /**
     * Force the validation factory to be created at static init time, so it is
     * bootstrapped in a JVM rather than in native-image
     *
     * TODO: we really only need to run in native-image
     *
     * @param provider
     */
    public void forceInit(InjectionInstance<ValidatorProvider> provider) {
        provider.newInstance().forceInit();
    }
}
