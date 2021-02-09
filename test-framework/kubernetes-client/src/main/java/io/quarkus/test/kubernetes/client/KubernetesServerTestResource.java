package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.net.InetAddress;
import java.util.Collections;

import io.fabric8.kubernetes.client.GenericKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class KubernetesServerTestResource extends AbstractKubernetesTestResource<KubernetesServer>
        implements QuarkusTestResourceConfigurableLifecycleManager<WithKubernetesTestServer> {

    private boolean https = false;
    private boolean crud = true;
    private int port = 0;

    @Override
    public void init(WithKubernetesTestServer annotation) {
        this.https = annotation.https();
        this.crud = annotation.crud();
        this.port = annotation.port();
    }

    @Override
    protected GenericKubernetesClient<?> getClient() {
        return server.getClient();
    }

    @Override
    protected void initServer() {
        server.before();
    }

    @Override
    protected KubernetesServer createServer() {
        return new KubernetesServer(https, crud, InetAddress.getLoopbackAddress(), port, Collections.emptyList());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.after();
            server = null;
        }
    }

    @Override
    protected Class<?> getInjectedClass() {
        return KubernetesServer.class;
    }

    @Override
    protected Class<? extends Annotation> getInjectionAnnotation() {
        return KubernetesTestServer.class;
    }
}
