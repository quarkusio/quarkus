package io.quarkus.resteasy.reactive.server.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.function.Function;

import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.test.TestHttpEndpointProvider;

public class ResteasyReactiveTestHttpProvider implements TestHttpEndpointProvider {
    @Override
    public Function<Class<?>, String> endpointProvider() {
        return new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                String value = getPath(aClass);
                if (value == null) {
                    return null;
                }
                if (value.startsWith("/")) {
                    value = value.substring(1);
                }
                //TODO: there is not really any way to handle @ApplicationPath, we could do something for @QuarkusTest apps but we can't for
                //native apps, so we just have to document the limitation
                String path = "/";
                Optional<String> appPath = getAppPath();
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

    private Optional<String> getAppPath() {
        Config config = ConfigProvider.getConfig();
        Optional<String> legacyProperty = config.getOptionalValue("quarkus.rest.path", String.class);
        if (legacyProperty.isPresent()) {
            return legacyProperty;
        }

        return config.getOptionalValue("quarkus.resteasy-reactive.path", String.class);
    }

    private String getPath(Class<?> aClass) {
        String value = null;
        for (Annotation annotation : aClass.getAnnotations()) {
            if (annotation.annotationType().getName().equals(Path.class.getName())) {
                try {
                    value = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                    break;
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (value == null) {
            for (Class<?> i : aClass.getInterfaces()) {
                value = getPath(i);
                if (value != null) {
                    break;
                }
            }
        }
        if (value == null) {
            if (aClass.getSuperclass() != Object.class) {
                value = getPath(aClass.getSuperclass());
            }
        }
        return value;
    }
}
