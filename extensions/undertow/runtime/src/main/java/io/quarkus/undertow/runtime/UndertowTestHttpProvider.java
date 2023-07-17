package io.quarkus.undertow.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.function.Function;

import jakarta.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.test.TestHttpEndpointProvider;

public class UndertowTestHttpProvider implements TestHttpEndpointProvider {
    @Override
    public Function<Class<?>, String> endpointProvider() {
        return new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                String value = null;
                for (Annotation annotation : aClass.getAnnotations()) {
                    if (annotation.annotationType().getName().equals(WebServlet.class.getName())) {
                        try {
                            String[] patterns = (String[]) annotation.annotationType().getMethod("urlPatterns")
                                    .invoke(annotation);
                            if (patterns.length > 0) {
                                value = patterns[0];
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (value == null) {
                    return null;
                }
                if (value.endsWith("/*")) {
                    value = value.substring(0, value.length() - 1);
                }
                if (value.startsWith("/")) {
                    value = value.substring(1);
                }
                String path = "/";
                Optional<String> appPath = ConfigProvider.getConfig().getOptionalValue("quarkus.servlet.context-path",
                        String.class);
                if (appPath.isPresent()) {
                    path = appPath.get();
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                value = path + value;
                return value;
            }
        };
    }
}
