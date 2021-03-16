package org.jboss.resteasy.reactive.spi;

import org.jboss.resteasy.reactive.common.core.AbstractResteasyReactiveContext;

public interface RestHandler<T extends AbstractResteasyReactiveContext> {

    void handle(T requestContext) throws Exception;

}
