package io.quarkus.qrs.runtime.spi;

public interface EndpointInvoker {

    Object invoke(Object instance, Object[] parameters) throws Exception;

}
