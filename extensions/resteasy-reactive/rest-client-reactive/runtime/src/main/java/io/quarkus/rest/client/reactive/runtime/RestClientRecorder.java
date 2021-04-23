package io.quarkus.rest.client.reactive.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public RuntimeValue<AnnotationRegisteredProviders> registeredProviders(
            Map<String, List<RegisteredProvider>> annotationsByClassName) {
        AnnotationRegisteredProviders result = new AnnotationRegisteredProviders();

        for (Map.Entry<String, List<RegisteredProvider>> providersForClass : annotationsByClassName.entrySet()) {
            result.addProviders(providersForClass.getKey(), toMapOfProviders(providersForClass.getValue()));
        }

        return new RuntimeValue<>(result);
    }

    private Map<Class<?>, Integer> toMapOfProviders(List<RegisteredProvider> value) {
        Map<Class<?>, Integer> result = new HashMap<>();
        for (RegisteredProvider registeredProvider : value) {
            try {
                result.put(Class.forName(registeredProvider.className, false, Thread.currentThread().getContextClassLoader()),
                        registeredProvider.priority);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static class RegisteredProvider {
        public String className;
        public int priority;
    }
}
