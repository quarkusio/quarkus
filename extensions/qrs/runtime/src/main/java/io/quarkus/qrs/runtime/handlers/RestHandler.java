package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

public interface RestHandler {

    void handle(QrsRequestContext requestContext) throws Exception;

}
