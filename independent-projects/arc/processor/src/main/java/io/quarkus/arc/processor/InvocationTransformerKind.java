package io.quarkus.arc.processor;

enum InvocationTransformerKind {
    INSTANCE,
    ARGUMENT,
    RETURN_VALUE,
    EXCEPTION,
    WRAPPER,
}
