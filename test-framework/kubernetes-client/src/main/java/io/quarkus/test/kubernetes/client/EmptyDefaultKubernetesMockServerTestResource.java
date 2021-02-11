package io.quarkus.test.kubernetes.client;

import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

/**
 * Kubernetes mock server, which responds to most of the basic api calls.
 */
public class EmptyDefaultKubernetesMockServerTestResource extends KubernetesMockServerTestResource {

    @Override
    public void configureMockServer(KubernetesMockServer mockServer) {
        final String ns = Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY;
        final String basePath = "/api/v1/namespaces/" + ns;

        mockServer.expect().get().withPath(basePath + "/configmaps")
                .andReturn(200, new ConfigMapList())
                .always();
        mockServer.expect().get().withPath(basePath + "/configmaps?watch=true")
                .andReturn(200, new ConfigMapList())
                .always();

        mockServer.expect().get().withPath(basePath + "/deployments")
                .andReturn(200, new DeploymentList())
                .always();
        mockServer.expect().get().withPath(basePath + "/deployments?watch=true")
                .andReturn(200, new DeploymentList())
                .always();

        mockServer.expect().get().withPath(basePath + "/events")
                .andReturn(200, new EventList())
                .always();
        mockServer.expect().get().withPath(basePath + "/events?watch=true")
                .andReturn(200, new EventList())
                .always();

        mockServer.expect().get().withPath("/apis/extensions/v1beta1/namespaces/" + ns + "/ingresses")
                .andReturn(200, new IngressList())
                .always();
        mockServer.expect().get().withPath("/apis/extensions/v1beta1/namespaces/" + ns + "/ingresses?watch=true")
                .andReturn(200, new IngressList())
                .always();

        mockServer.expect().get().withPath(basePath + "/pods")
                .andReturn(200, new PodList())
                .always();
        mockServer.expect().get().withPath(basePath + "/pods?watch=true")
                .andReturn(200, new PodList())
                .always();

        mockServer.expect().get().withPath(basePath + "/serviceaccounts")
                .andReturn(200, new ServiceAccountList())
                .always();
        mockServer.expect().get().withPath(basePath + "/serviceaccounts?watch=true")
                .andReturn(200, new ServiceAccountList())
                .always();

        mockServer.expect().get().withPath(basePath + "/services")
                .andReturn(200, new ServiceList())
                .always();
        mockServer.expect().get().withPath(basePath + "/services?watch=true")
                .andReturn(200, new ServiceList())
                .always();
    }

}
