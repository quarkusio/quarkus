package io.quarkus.test.kubernetes.client;

import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;

public class OpenShiftMockServerTestResource extends KubernetesMockServerTestResource {

    protected OpenShiftMockServer createMockServer() {
        return new OpenShiftMockServer(useHttps());
    }

    @Override
    protected Class<?> getInjectedClass() {
        return OpenShiftMockServer.class;
    }
}
