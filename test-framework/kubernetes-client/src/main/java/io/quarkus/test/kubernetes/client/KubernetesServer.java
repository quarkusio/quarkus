package io.quarkus.test.kubernetes.client;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import io.fabric8.mockwebserver.http.RecordedRequest;

public class KubernetesServer {

    private KubernetesMockServer kubernetesMockServer;
    private NamespacedKubernetesClient client;
    private final boolean https;
    private final boolean crudMode;
    private final InetAddress address;
    private final int port;
    private final List<CustomResourceDefinitionContext> crdContextList;

    public KubernetesServer(boolean https, boolean crudMode, InetAddress address, int port,
            List<CustomResourceDefinitionContext> crdContextList) {
        this.https = https;
        this.crudMode = crudMode;
        this.address = address;
        this.port = port;
        this.crdContextList = crdContextList;
    }

    public final void before() {
        final Map<ServerRequest, Queue<ServerResponse>> responses = new HashMap<>();
        kubernetesMockServer = crudMode
                ? new KubernetesMockServer(new Context(), new MockWebServer(), responses,
                        new KubernetesMixedDispatcher(responses, crdContextList), https)
                : new KubernetesMockServer(https);
        kubernetesMockServer.init(address, port);
        client = kubernetesMockServer.createClient();
    }

    public final void after() {
        kubernetesMockServer.destroy();
        client.close();
    }

    public final NamespacedKubernetesClient getClient() {
        return client;
    }

    public final MockServerExpectation expect() {
        return kubernetesMockServer.expect();
    }

    public final KubernetesMockServer getKubernetesMockServer() {
        return kubernetesMockServer;
    }

    public final RecordedRequest getLastRequest() throws InterruptedException {
        return kubernetesMockServer.getLastRequest();
    }
}
