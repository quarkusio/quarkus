package org.jboss.resteasy.reactive.common.runtime.core;

public interface RestHandler<T extends AbstractResteasyReactiveContext> {

    void handle(T requestContext) throws Exception;

}
