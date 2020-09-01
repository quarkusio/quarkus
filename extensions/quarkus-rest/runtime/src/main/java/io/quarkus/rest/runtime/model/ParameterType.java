package io.quarkus.rest.runtime.model;

public enum ParameterType {

    PATH,
    QUERY,
    HEADER,
    FORM,
    BODY,
    CONTEXT,
    ASYNC_RESPONSE
}
