package org.jboss.resteasy.reactive.server.spi;

public interface EndpointInvoker {

    Object invoke(Object instance, Object[] parameters) throws Exception;

}
