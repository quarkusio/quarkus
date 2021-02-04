package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.client.GenericKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

public class KubernetesServerTestResource extends AbstractKubernetesTestResource<KubernetesServer> {

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
        return new KubernetesServer(useHttps(), true);
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
        return Server.class;
    }
}
