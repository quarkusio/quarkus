package io.quarkus.deployment.recording;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.proxy.ProxyFactory;

class RecordingProxyFactories {

    private static final Map<Class<?>, ProxyFactory<?>> RECORDING_PROXY_FACTORIES = new ConcurrentHashMap<>();

    static <T> void put(Class<T> clazz, ProxyFactory<T> proxyFactory) {
        RECORDING_PROXY_FACTORIES.put(clazz, proxyFactory);

        ClassLoader proxyClassLoader = proxyFactory.getClassLoader();
        if (proxyClassLoader instanceof QuarkusClassLoader) {
            ((QuarkusClassLoader) proxyClassLoader).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    RecordingProxyFactories.RECORDING_PROXY_FACTORIES.remove(clazz);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    static <T> ProxyFactory<T> get(Class<T> clazz) {
        return (ProxyFactory<T>) RECORDING_PROXY_FACTORIES.get(clazz);
    }
}
