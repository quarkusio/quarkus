package org.jboss.resteasy.reactive.server.spi;

/**
 * Base interface implemented by the synthetic beans that represent rest endpoints.
 *
 * @see CoroutineEndpointInvoker
 */
public interface EndpointInvoker {

    /**
     * Delegates control over the bean that defines the endpoint
     * 
     * @param instance the bean instance
     * @param parameters the method arguments
     * @return the result of the method call
     * @throws Exception the exception thrown in the bean call
     */
    Object invoke(Object instance, Object[] parameters) throws Exception;
}
