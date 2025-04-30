package io.quarkus.jaxrs.client.reactive.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.jboss.resteasy.reactive.client.impl.ClientProxies;
import org.jboss.resteasy.reactive.client.impl.ClientSerialisers;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;

import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaxrsClientReactiveRecorder extends ResteasyReactiveCommonRecorder {

    private static volatile Serialisers serialisers;
    private static volatile GenericTypeMapping genericTypeMapping;
    private static volatile Map<Class<?>, MultipartResponseData> multipartResponsesData;

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

    public static Map<Class<?>, MultipartResponseData> getMultipartResponsesData() {
        return multipartResponsesData;
    }

    public void setMultipartResponsesData(Map<String, RuntimeValue<MultipartResponseData>> multipartResponsesData) {
        Map<Class<?>, MultipartResponseData> runtimeMap = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<MultipartResponseData>> multipartData : multipartResponsesData.entrySet()) {
            runtimeMap.put(loadClass(multipartData.getKey()), multipartData.getValue().getValue());
        }

        JaxrsClientReactiveRecorder.multipartResponsesData = runtimeMap;
    }

    public void setupClientProxies(
            Map<String, RuntimeValue<BiFunction<WebTarget, List<ParamConverterProvider>, ?>>> clientImplementations,
            Map<String, String> failures) {
        clientProxies = createClientImpls(clientImplementations, failures);
    }

    public Serialisers createSerializers() {
        ClientSerialisers s = new ClientSerialisers();
        s.registerBuiltins(RuntimeType.CLIENT);
        serialisers = s;
        return s;
    }

    private ClientProxies createClientImpls(
            Map<String, RuntimeValue<BiFunction<WebTarget, List<ParamConverterProvider>, ?>>> clientImplementations,
            Map<String, String> failureMessages) {
        Map<Class<?>, BiFunction<WebTarget, List<ParamConverterProvider>, ?>> map = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<BiFunction<WebTarget, List<ParamConverterProvider>, ?>>> entry : clientImplementations
                .entrySet()) {
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
