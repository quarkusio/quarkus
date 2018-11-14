package org.jboss.protean.arc.processor;

import org.jboss.protean.arc.InjectableBean;

/**
 * Allows a build-time extension to register synthetic {@link InjectableBean} implementations.
 *
 * @author Martin Kouba
 */
public interface BeanRegistrar extends BuildExtension {

    /**
     *
     * @param registrationContext
     */
    void register(RegistrationContext registrationContext);


    interface RegistrationContext extends BuildContext {

        /**
         *
         * @param beanClass
         * @return a new synthetic bean builder
         */
        <T> BeanConfigurator<T> configure(Class<?> beanClass);

        // TODO add synthetic observer?

    }

}
