package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.GenericKubernetesClient;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class AbstractKubernetesTestResource<T> implements QuarkusTestResourceLifecycleManager {
    protected T server;

    @Override
    public Map<String, String> start() {
        final Map<String, String> systemProps = new HashMap<>();
        systemProps.put(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        systemProps.put(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        systemProps.put(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
        systemProps.put(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "test");
        systemProps.put(Config.KUBERNETES_HTTP2_DISABLE, "true");

        server = createServer();
        initServer();

        try (GenericKubernetesClient<?> client = getClient()) {
            systemProps.put(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, client.getConfiguration().getMasterUrl());
        }

        configureServer();
        //these actually need to be system properties
        //as they are read directly as system props, and not from Quarkus config
        for (Map.Entry<String, String> entry : systemProps.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        return systemProps;
    }

    protected abstract GenericKubernetesClient<?> getClient();

    /**
     * Can be used by subclasses in order to
     * setup the mock server before the Quarkus application starts
     */
    protected void configureServer() {
    }

    protected void initServer() {
    }

    protected abstract T createServer();

    protected boolean useHttps() {
        return Boolean.getBoolean("quarkus.kubernetes-client.test.https");
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(server,
                new TestInjector.AnnotatedAndMatchesType(getInjectionAnnotation(), getInjectedClass()));
    }

    protected abstract Class<?> getInjectedClass();

    protected abstract Class<? extends Annotation> getInjectionAnnotation();

}
