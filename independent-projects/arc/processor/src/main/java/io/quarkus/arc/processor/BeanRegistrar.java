package io.quarkus.arc.processor;

import io.quarkus.arc.InjectableBean;
import org.jboss.jandex.DotName;

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
         * @return a new synthetic bean configurator
         */
        <T> BeanConfigurator<T> configure(DotName beanClassName);

        /**
         * 
         * @param beanClass
         * @return a new synthetic bean configurator
         */
        default <T> BeanConfigurator<T> configure(Class<?> beanClass) {
            return configure(DotName.createSimple(beanClass.getName()));
        }

    }

}
