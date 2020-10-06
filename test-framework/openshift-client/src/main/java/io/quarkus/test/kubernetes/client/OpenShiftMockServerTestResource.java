package io.quarkus.test.kubernetes.client;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;

public class OpenShiftMockServerTestResource extends KubernetesMockServerTestResource {

    protected KubernetesMockServer createMockServer() {
        return new OpenShiftMockServer(useHttps());
    }
}
