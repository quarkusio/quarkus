package org.jboss.protean.arc.processor;

import org.jboss.protean.arc.InjectableBean;

/**
 * Allows a build-time extension to register synthetic {@link InjectableBean} implementations.
 *
 * @author Martin Kouba
 */
public interface BeanRegistrar extends BuildProcessor {

    /**
     *
     * @param registrationContext
     */
    void register(RegistrationContext registrationContext);


    interface RegistrationContext {

        /**
         *
         * @param implementationClass
         * @return a new synthetic bean builder
         */
        <T> BeanConfigurator<T> configure(Class<T> implementationClass);

    }

}
