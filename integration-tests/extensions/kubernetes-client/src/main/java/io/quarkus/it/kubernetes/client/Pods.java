package io.quarkus.it.kubernetes.client;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@Path("/pod")
public class Pods {

    private final KubernetesClient kubernetesClient;

    public Pods(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{namespace}")
    public List<Pod> pods(@PathParam("namespace") String namespace) {
        return kubernetesClient.pods().inNamespace(namespace).list().getItems();
    }

    @DELETE
    @Path("/{namespace}")
    public Response deleteFirst(@PathParam("namespace") String namespace) {
        final List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
        if (pods.isEmpty()) {
            return Response.status(404).build();
        }

        kubernetesClient.pods().inNamespace(namespace).delete(pods.get(0));
        return Response.noContent().build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{namespace}")
    public Response updateFirst(@PathParam("namespace") String namespace) {
        final List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
        if (pods.isEmpty()) {
            return Response.status(404).build();
        }

        final Pod pod = pods.get(0);
        final String podName = pod.getMetadata().getName();
        // would normally do some kind of meaningful update here
        Pod updatedPod = new PodBuilder().withNewMetadata().withName(podName).withNewResourceVersion("12345").endMetadata()
                .build();

        updatedPod = kubernetesClient.pods().withName(podName).createOrReplace(updatedPod);
        return Response.ok(updatedPod).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{namespace}")
    public Pod createNew(@PathParam("namespace") String namespace) {
        return kubernetesClient.pods().inNamespace(namespace).createNew().withNewMetadata().withResourceVersion("12345")
                .endMetadata().done();
    }
}
