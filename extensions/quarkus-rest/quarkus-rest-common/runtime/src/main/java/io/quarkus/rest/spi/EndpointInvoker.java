package io.quarkus.rest.spi;

public interface EndpointInvoker {

    Object invoke(Object instance, Object[] parameters) throws Exception;

}
