package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import io.fabric8.kubernetes.client.GenericKubernetesClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class OpenShiftServerTestResource extends AbstractKubernetesTestResource<OpenShiftServer, NamespacedOpenShiftClient>
        implements QuarkusTestResourceConfigurableLifecycleManager<WithOpenShiftTestServer> {

    private boolean https = false;
    private boolean crud = true;
    private Consumer<OpenShiftServer> setup;

    @Override
    public void init(WithOpenShiftTestServer annotation) {
        this.https = annotation.https();
        this.crud = annotation.crud();
        try {
            this.setup = annotation.setup().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected GenericKubernetesClient<NamespacedOpenShiftClient> getClient() {
        return server.getOpenshiftClient();
    }

    @Override
    protected void initServer() {
        server.before();
    }

    @Override
    protected void configureServer() {
        if (setup != null)
            setup.accept(server);
    }

    @Override
    protected OpenShiftServer createServer() {
        return new OpenShiftServer(https, crud);
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
        return OpenShiftServer.class;
    }

    @Override
    protected Class<? extends Annotation> getInjectionAnnotation() {
        return OpenShiftTestServer.class;
    }
}
