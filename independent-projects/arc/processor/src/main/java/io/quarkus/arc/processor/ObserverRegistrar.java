package io.quarkus.arc.processor;

/**
 * Allows a build-time extension to register synthetic observers.
 *
 * @author Martin Kouba
 */
public interface ObserverRegistrar extends BuildExtension {

    /**
     *
     * @param context
     */
    void register(RegistrationContext context);

    interface RegistrationContext extends BuildContext {

        /**
         * Configura a new synthetic observer. The observer is not added to the deployment unless the
         * {@link ObserverConfigurator#done()} method is called.
         * 
         * @return a new synthetic observer configurator
         */
        ObserverConfigurator configure();

        /**
         * The returned stream contains all non-synthetic beans (beans derived from classes) and beans
         * registered by other {@link ObserverRegistrar}s before the stream is created.
         * 
         * @return a new stream of beans
         */
        BeanStream beans();

    }

}
