package io.quarkus.rest.runtime.model;

public enum ParameterType {

    PATH,
    QUERY,
    HEADER,
    FORM,
    BODY,
    MATRIX,
    CONTEXT,
    ASYNC_RESPONSE,
    COOKIE,
    BEAN
}
