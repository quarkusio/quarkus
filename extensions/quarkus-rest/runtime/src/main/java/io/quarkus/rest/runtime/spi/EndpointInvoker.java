package io.quarkus.rest.runtime.spi;

public interface EndpointInvoker {

    Object invoke(Object instance, Object[] parameters) throws Exception;

}
