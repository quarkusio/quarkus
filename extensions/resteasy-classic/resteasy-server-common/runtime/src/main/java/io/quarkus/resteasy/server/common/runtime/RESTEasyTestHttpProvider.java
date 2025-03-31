package io.quarkus.resteasy.server.common.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.test.TestHttpEndpointProvider;

public class RESTEasyTestHttpProvider implements TestHttpEndpointProvider {
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

                String path = "/";
                Instance<Application> application = CDI.current().select(Application.class);
                if (application.isResolvable()) {
                    path = application.get().getClass().getDeclaredAnnotation(ApplicationPath.class).value();
                } else {
                    Optional<String> appPath = ConfigProvider.getConfig().getOptionalValue("quarkus.resteasy.path",
                            String.class);
                    if (appPath.isPresent()) {
                        path = appPath.get();
                    }
                }

                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                value = path + value;
                return value;
            }
        };
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
