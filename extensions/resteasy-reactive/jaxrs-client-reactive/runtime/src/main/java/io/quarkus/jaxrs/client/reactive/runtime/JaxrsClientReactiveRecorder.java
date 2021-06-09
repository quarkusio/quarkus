package io.quarkus.jaxrs.client.reactive.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.WebTarget;

import org.jboss.resteasy.reactive.client.impl.ClientProxies;
import org.jboss.resteasy.reactive.client.impl.ClientSerialisers;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;

import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaxrsClientReactiveRecorder extends ResteasyReactiveCommonRecorder {

    private static volatile Serialisers serialisers;
    private static volatile GenericTypeMapping genericTypeMapping;

    private static volatile ClientProxies clientProxies = new ClientProxies(Collections.emptyMap(), Collections.emptyMap());

    public static ClientProxies getClientProxies() {
        return clientProxies;
    }

    public static Serialisers getSerialisers() {
        return serialisers;
    }

    public static GenericTypeMapping getGenericTypeMapping() {
        return genericTypeMapping;
    }

    public void setupClientProxies(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations,
            Map<String, String> failures) {
        clientProxies = createClientImpls(clientImplementations, failures);
    }

    public Serialisers createSerializers() {
        ClientSerialisers s = new ClientSerialisers();
        s.registerBuiltins(RuntimeType.CLIENT);
        serialisers = s;
        return s;
    }

    private ClientProxies createClientImpls(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations,
            Map<String, String> failureMessages) {
        Map<Class<?>, Function<WebTarget, ?>> map = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<Function<WebTarget, ?>>> entry : clientImplementations.entrySet()) {
            map.put(loadClass(entry.getKey()), entry.getValue().getValue());
        }
        Map<Class<?>, String> failures = new HashMap<>();
        for (Map.Entry<String, String> entry : failureMessages.entrySet()) {
            failures.put(loadClass(entry.getKey()), entry.getValue());
        }

        return new ClientProxies(map, failures);
    }

    public void setGenericTypeMapping(GenericTypeMapping typeMapping) {
        genericTypeMapping = typeMapping;
    }

    public void registerInvocationHandlerGenericType(GenericTypeMapping genericTypeMapping, String invocationHandlerClass,
            String resolvedType) {
        genericTypeMapping.addInvocationCallback(loadClass(invocationHandlerClass), loadClass(resolvedType));
    }
}
