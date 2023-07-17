package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.ext.ParamConverter;

public class MyParameterConverter implements ParamConverter<MyParameter> {

    @Override
    public MyParameter fromString(String value) {
        return new MyParameter(value, value);
    }

    @Override
    public String toString(MyParameter value) {
        return value.toString();
    }

}
