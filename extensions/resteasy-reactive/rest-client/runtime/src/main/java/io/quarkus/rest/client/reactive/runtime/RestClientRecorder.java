package io.quarkus.rest.client.reactive.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.http.HttpClient;

@Recorder
public class RestClientRecorder {
    private static volatile Map<String, String> configKeys;
    private static volatile Set<String> blockingClassNames;
    private static volatile Set<String> interfacesWithUrlParam = Set.of();

    private static final Map<String, List<HttpClient>> tlsConfigNameToVertxHttpClients = new ConcurrentHashMap<>();

    public void setConfigKeys(Map<String, String> configKeys) {
        RestClientRecorder.configKeys = configKeys;
    }

    public void setBlockingClassNames(Set<String> blockingClassNames) {
        RestClientRecorder.blockingClassNames = blockingClassNames;
    }

    public static Map<String, String> getConfigKeys() {
        return configKeys;
    }

    public static boolean isClassBlocking(Class<?> exceptionMapperClass) {
        return blockingClassNames.contains(exceptionMapperClass.getName());
    }

    public void setInterfacesWithUrlParam(Set<String> interfacesWithUrlParam) {
        RestClientRecorder.interfacesWithUrlParam = interfacesWithUrlParam;
    }

    /**
     * @return whether the client interface, or one of the interfaces it extends, declares a method parameter annotated
     *         with {@code @Url}
     */
    public static boolean hasUrlParam(Class<?> clientInterface) {
        if (interfacesWithUrlParam.contains(clientInterface.getName())) {
            return true;
        }
        for (Class<?> superInterface : clientInterface.getInterfaces()) {
            if (hasUrlParam(superInterface)) {
                return true;
            }
        }
        return false;
    }

    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public static void registerReloadableHttpClient(String tlsConfigName, HttpClient httpClient) {
        tlsConfigNameToVertxHttpClients.computeIfAbsent(tlsConfigName, new Function<>() {
            @Override
            public List<HttpClient> apply(String s) {
                return new ArrayList<>(1);
            }
        }).add(httpClient);
    }

    public static List<HttpClient> clientsUsingTlsConfig(String tlsConfigName) {
        return tlsConfigNameToVertxHttpClients.getOrDefault(tlsConfigName, Collections.emptyList());
    }

    public static void removeClientFromTlsConfigMap(String tlsConfigName, HttpClient httpClient) {
        if (tlsConfigName == null) {
            return;
        }
        List<HttpClient> httpClients = tlsConfigNameToVertxHttpClients.get(tlsConfigName);
        if (httpClients == null) {
            return;
        }
        httpClients.remove(httpClient);
    }

    public void cleanUp(ShutdownContext shutdown) {
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                tlsConfigNameToVertxHttpClients.clear();
            }
        });
    }

}
