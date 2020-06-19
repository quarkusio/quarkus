package io.quarkus.qrs.runtime.core;

public interface RestHandler {

    void handle(RequestContext requestContext) throws Exception;

}
