package io.quarkus.resteasy.reactive.server.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.server.core.Deployment;

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
        try {
            Class<?> recorderFromTCCL = Thread.currentThread().getContextClassLoader()
                    .loadClass(ResteasyReactiveRecorder.class.getName());
            Method getCurrentDeploymentMethod = recorderFromTCCL.getMethod("getCurrentDeployment");
            Object deploymentObject = getCurrentDeploymentMethod.invoke(null);
            Class<?> deploymentFromTCCL = Thread.currentThread().getContextClassLoader().loadClass(Deployment.class.getName());
            Method getPrefixMethod = deploymentFromTCCL.getMethod("getPrefix");
            String prefix = (String) getPrefixMethod.invoke(deploymentObject);
            return Optional.of(prefix);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to obtain app app", e);
        }
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
