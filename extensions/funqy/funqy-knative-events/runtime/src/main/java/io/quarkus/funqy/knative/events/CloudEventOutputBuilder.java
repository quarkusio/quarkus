package io.quarkus.funqy.knative.events;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class CloudEventOutputBuilder {

    public static class GenericType<T> {
        protected GenericType() {

        }
    }

    private CloudEventOutput event = new CloudEventOutput();

    public CloudEventOutputBuilder type(String type) {
        event.type = type;
        return this;
    }

    public CloudEventOutputBuilder source(String source) {
        event.source = source;
        return this;
    }

    public CloudEventOutputBuilder javaType(Type javaType) {
        event.javaType = javaType;
        return this;
    }

    public CloudEventOutputBuilder javaType(GenericType generic) {
        ParameterizedType pt = (ParameterizedType) generic.getClass().getGenericSuperclass();
        event.javaType = pt.getActualTypeArguments()[0];
        return this;
    }

    public <T> CloudEventOutput<T> build(T data) {
        event.data = data;
        return event;
    }
}
