package org.jboss.resteasy.reactive.spi;

public interface EndpointInvoker {

    Object invoke(Object instance, Object[] parameters) throws Exception;

}
