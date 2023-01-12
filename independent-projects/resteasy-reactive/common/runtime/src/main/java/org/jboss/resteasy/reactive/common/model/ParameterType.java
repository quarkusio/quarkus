package org.jboss.resteasy.reactive.common.model;

public enum ParameterType {

    PATH,
    QUERY,
    HEADER,
    FORM,
    MULTI_PART_FORM,
    MULTI_PART_DATA_INPUT,
    BODY,
    MATRIX,
    CONTEXT,
    ASYNC_RESPONSE,
    COOKIE,
    BEAN,
    CUSTOM
}
