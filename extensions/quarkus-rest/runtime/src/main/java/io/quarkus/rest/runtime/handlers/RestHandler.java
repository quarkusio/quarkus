package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.AbstractQuarkusRestContext;

public interface RestHandler<T extends AbstractQuarkusRestContext> {

    void handle(T requestContext) throws Exception;

}
