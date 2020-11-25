package org.jboss.resteasy.reactive.common.core;

public interface RestHandler<T extends AbstractResteasyReactiveContext> {

    void handle(T requestContext) throws Exception;

}
