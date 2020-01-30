package io.quarkus.arc.processor;

import org.jboss.jandex.DotName;

/**
 * Allows a build-time extension to register synthetic beans and observers.
 *
 * @author Martin Kouba
 */
public interface BeanRegistrar extends BuildExtension {

    /**
     *
     * @param context
     */
    void register(RegistrationContext context);

    interface RegistrationContext extends BuildContext {

        /**
         * The synthetic bean is not added to the deployment unless the {@link BeanConfigurator#done()} method is called.
         *
         * @param beanClass
         * @return a new synthetic bean configurator
         */
        <T> BeanConfigurator<T> configure(DotName beanClassName);

        /**
         * The synthetic bean is not added to the deployment unless the {@link BeanConfigurator#done()} method is called.
         * 
         * @param beanClass
         * @return a new synthetic bean configurator
         */
        default <T> BeanConfigurator<T> configure(Class<?> beanClass) {
            return configure(DotName.createSimple(beanClass.getName()));
        }

        /**
         * The synthetic observer is not added to the deployment unless the {@link ObserverConfigurator#done()} method is
         * called.
         * 
         * @return a new synthetic observer configurator
         */
        ObserverConfigurator configureObserver();

        /**
         * The returned stream contains all non-synthetic beans (beans derived from classes) and beans
         * registered by other {@link BeanRegistrar}s before the stream is created.
         * 
         * @return a new stream of beans
         */
        BeanStream beans();

    }

}
