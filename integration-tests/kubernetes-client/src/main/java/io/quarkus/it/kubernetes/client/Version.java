package io.quarkus.it.kubernetes.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.fabric8.kubernetes.client.KubernetesClient;

@Path("/version")
public class Version {

    private final KubernetesClient kubernetesClient;

    public Version(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @GET
    public Response version() {
        return Response.ok(kubernetesClient.getKubernetesVersion().getPlatform()).build();
    }
}
