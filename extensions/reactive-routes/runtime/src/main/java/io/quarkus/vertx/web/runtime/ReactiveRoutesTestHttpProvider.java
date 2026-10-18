package io.quarkus.vertx.web.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.vertx.web.RouteBase;

public class ReactiveRoutesTestHttpProvider implements TestHttpEndpointProvider {
    @Override
    public Function<Class<?>, String> endpointProvider() {
        return new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                String value = null;
                for (Annotation annotation : aClass.getAnnotations()) {
                    if (annotation.annotationType().getName().equals(RouteBase.class.getName())) {
                        try {
                            value = (String) annotation.annotationType().getMethod("path").invoke(annotation);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if ((value == null) || value.isEmpty()) {
                    return null;
                }
                if (!value.startsWith("/")) {
                    value = "/" + value;
                }
                return value;
            }
        };
    }
}
