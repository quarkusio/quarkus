package io.quarkus.spring.web.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;
import org.springframework.web.bind.annotation.RequestMapping;

import io.quarkus.runtime.test.TestHttpEndpointProvider;

public class SpringWebEndpointProvider implements TestHttpEndpointProvider {
    @Override
    public Function<Class<?>, String> endpointProvider() {
        return new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                String[] paths = getPath(aClass);
                if (paths == null) {
                    return null;
                }
                String value;
                if (paths.length == 0) {
                    value = "";
                } else {
                    value = paths[0];
                    if (value.startsWith("/")) {
                        value = value.substring(1);
                    }
                }
                //TODO: there is not really any way to handle @ApplicationPath, we could do something for @QuarkusTest apps but we can't for
                //native apps, so we just have to document the limitation
                String path = "/";
                Optional<String> appPath = ConfigProvider.getConfig().getOptionalValue("quarkus.resteasy.path", String.class);
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

    private String[] getPath(Class<?> aClass) {
        String[] value = null;
        for (Annotation annotation : aClass.getAnnotations()) {
            if (annotation.annotationType().getName().equals(RequestMapping.class.getName())) {
                try {
                    value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
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
