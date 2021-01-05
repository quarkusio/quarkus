package io.quarkus.funqy.knative.events;

import java.lang.reflect.Type;

public class CloudEventOutput<T> {
    Type javaType;
    String type;
    String source;
    T data;

    protected CloudEventOutput() {

    }

    public Type javaType() {
        return javaType;
    }

    public String type() {
        return type;
    }

    public String source() {
        return source;
    }

    public T data() {
        return data;
    }

}