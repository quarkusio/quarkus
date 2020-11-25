package io.quarkus.resteasy.reactive.client.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.WebTarget;

import org.jboss.resteasy.reactive.client.ClientProxies;
import org.jboss.resteasy.reactive.client.ClientSerialisers;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;

import io.quarkus.resteasy.reactive.common.runtime.QuarkusRestCommonRecorder;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyReactiveClientRecorder extends QuarkusRestCommonRecorder {

    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };
    private static volatile Serialisers serialisers;
    private static volatile GenericTypeMapping genericTypeMapping;

    private static volatile ClientProxies clientProxies = new ClientProxies(Collections.emptyMap());

    public static ClientProxies getClientProxies() {
        return clientProxies;
    }

    public static Serialisers getSerialisers() {
        return serialisers;
    }

    public static GenericTypeMapping getGenericTypeMapping() {
        return genericTypeMapping;
    }

    public void setupClientProxies(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations) {
        clientProxies = createClientImpls(clientImplementations);
    }

    public Serialisers createSerializers() {
        ClientSerialisers s = new ClientSerialisers();
        s.registerBuiltins(RuntimeType.CLIENT);
        serialisers = s;
        return s;
    }

    private ClientProxies createClientImpls(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations) {
        Map<Class<?>, Function<WebTarget, ?>> map = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<Function<WebTarget, ?>>> entry : clientImplementations.entrySet()) {
            map.put(loadClass(entry.getKey()), entry.getValue().getValue());
        }
        return new ClientProxies(map);
    }

    public void setGenericTypeMapping(GenericTypeMapping typeMapping) {
        genericTypeMapping = typeMapping;
    }

    public void registerInvocationHandlerGenericType(GenericTypeMapping genericTypeMapping, String invocationHandlerClass,
            String resolvedType) {
        genericTypeMapping.addInvocationCallback(loadClass(invocationHandlerClass), loadClass(resolvedType));
    }
}
