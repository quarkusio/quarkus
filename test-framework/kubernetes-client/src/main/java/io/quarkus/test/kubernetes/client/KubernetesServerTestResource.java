package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

import io.fabric8.kubernetes.client.GenericKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

public class KubernetesServerTestResource extends AbstractKubernetesTestResource<KubernetesServer> {

    /**
     * Configure HTTPS usage, defaults to false
     */
    public final static String HTTPS = "https";
    /**
     * Configure CRUD usage, defaults to true
     */
    public final static String CRUD = "crud";
    /**
     * Configure the port to use, defaults to 0, for the first available port
     */
    public final static String PORT = "port";

    private boolean https = false;
    private boolean crud = true;
    private int port = 0;

    @Override
    public void init(Map<String, String> initArgs) {
        String val = initArgs.get(HTTPS);
        if (val != null) {
            this.https = Boolean.parseBoolean(val);
        }
        val = initArgs.get(CRUD);
        if (val != null) {
            this.crud = Boolean.parseBoolean(val);
        }
        val = initArgs.get(PORT);
        if (val != null) {
            this.port = Integer.parseInt(val);
        }
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
