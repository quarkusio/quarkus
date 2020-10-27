package io.quarkus.rest.common.runtime.core;

public interface RestHandler<T extends AbstractQuarkusRestContext> {

    void handle(T requestContext) throws Exception;

}
