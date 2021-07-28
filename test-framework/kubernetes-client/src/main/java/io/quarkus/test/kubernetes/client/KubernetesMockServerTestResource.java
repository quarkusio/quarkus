package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.client.GenericKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

/**
 * @deprecated use {@link KubernetesServerTestResource}
 */
@Deprecated
public class KubernetesMockServerTestResource
        extends AbstractKubernetesTestResource<KubernetesMockServer, NamespacedKubernetesClient> {

    @Override
    protected GenericKubernetesClient<NamespacedKubernetesClient> getClient() {
        return server.createClient();
    }

    @Override
    protected void initServer() {
        server.init();
    }

    @Override
    protected KubernetesMockServer createServer() {
        return createMockServer();
    }

    /**
     * @deprecated use {@link #createServer()}
     */
    @Deprecated
    protected KubernetesMockServer createMockServer() {
        return new KubernetesMockServer(useHttps());
    }

    @Override
    protected void configureServer() {
        configureMockServer(server);
    }

    /**
     * @deprecated use {@link #configureServer()}
     */
    @Deprecated
    public void configureMockServer(KubernetesMockServer mockServer) {
    }

    @Override
    public void stop() {
        if (server != null) {
            server.destroy();
            server = null;
        }
    }

    @Override
    protected Class<?> getInjectedClass() {
        return KubernetesMockServer.class;
    }

    @Override
    protected Class<? extends Annotation> getInjectionAnnotation() {
        return MockServer.class;
    }
}
