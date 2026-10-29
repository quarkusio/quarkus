package io.quarkus.jaxrs.client.reactive.runtime;

import static io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder.loadClass;

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

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.resteasy.reactive.common.runtime.SerializerDefinition;

public class JaxrsClientReactiveRecorder {

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

    /**
     * Sets up the client context: the client serialisers, the generated client implementations and the generated
     * multipart response data.
     *
     * @param beanContainer the bean container used to look up the reader and writer beans
     * @param readers the readers to register
     * @param writers the writers to register
     * @param clientImplementations the names of the generated client implementation creators, by client interface name
     * @param failures the reasons a client implementation could not be generated, by client interface name
     * @param multipartResponsesData the names of the generated multipart response data classes, by response type name
     */
    public static void setup(BeanContainer beanContainer, List<SerializerDefinition> readers,
            List<SerializerDefinition> writers, Map<String, String> clientImplementations, Map<String, String> failures,
            Map<String, String> multipartResponsesData) {
        ClientSerialisers clientSerialisers = new ClientSerialisers();
        clientSerialisers.registerBuiltins(RuntimeType.CLIENT);
        for (SerializerDefinition reader : readers) {
            clientSerialisers.addReader(loadClass(reader.handledClassName()), reader.reader(beanContainer));
        }
        for (SerializerDefinition writer : writers) {
            clientSerialisers.addWriter(loadClass(writer.handledClassName()), writer.writer(beanContainer));
        }
        serialisers = clientSerialisers;

        clientProxies = createClientImpls(clientImplementations, failures);

        Map<Class<?>, MultipartResponseData> runtimeMap = new HashMap<>();
        for (Map.Entry<String, String> multipartData : multipartResponsesData.entrySet()) {
            runtimeMap.put(loadClass(multipartData.getKey()), newInstance(multipartData.getValue()));
        }
        JaxrsClientReactiveRecorder.multipartResponsesData = runtimeMap;
    }

    private static ClientProxies createClientImpls(Map<String, String> clientImplementations,
            Map<String, String> failureMessages) {
        Map<Class<?>, BiFunction<WebTarget, List<ParamConverterProvider>, ?>> map = new HashMap<>();
        for (Map.Entry<String, String> entry : clientImplementations.entrySet()) {
            map.put(loadClass(entry.getKey()), newInstance(entry.getValue()));
        }
        Map<Class<?>, String> failures = new HashMap<>();
        for (Map.Entry<String, String> entry : failureMessages.entrySet()) {
            failures.put(loadClass(entry.getKey()), entry.getValue());
        }

        return new ClientProxies(map, failures);
    }

    /**
     * @param invocationCallbackTypes the type each {@code InvocationCallback} implementation is parameterized with, by
     *        implementation class name
     */
    public static void setGenericTypeMapping(Map<String, String> invocationCallbackTypes) {
        GenericTypeMapping typeMapping = new GenericTypeMapping();
        for (Map.Entry<String, String> invocationCallback : invocationCallbackTypes.entrySet()) {
            typeMapping.addInvocationCallback(loadClass(invocationCallback.getKey()),
                    loadClass(invocationCallback.getValue()));
        }
        genericTypeMapping = typeMapping;
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(String className) {
        try {
            return (T) loadClass(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + className, e);
        }
    }
}
