package io.quarkus.it.kubernetes.client;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;

@Path("/pod")
public class Pods {

    private final KubernetesClient kubernetesClient;

    @Inject
    public Pods(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @GET
    @Path("/{namespace}")
    public Response pods(@PathParam("namespace") String namespace) {
        return Response.ok(kubernetesClient.pods().inNamespace(namespace).list().getItems()).build();
    }

    @DELETE
    @Path("/{namespace}")
    public Response deleteFirst(@PathParam("namespace") String namespace) {
        final List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
        if (pods.isEmpty()) {
            return Response.status(404).build();
        }

        kubernetesClient.pods().inNamespace(namespace).resource(pods.get(0)).delete();
        return Response.noContent().build();
    }

    @PUT
    @Path("/{namespace}")
    public Response updateFirst(@PathParam("namespace") String namespace) {
        final List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
        if (pods.isEmpty()) {
            return Response.status(404).build();
        }

        final Pod pod = pods.get(0);
        final String podName = pod.getMetadata().getName();
        // would normally do some kind of meaningful update here
        Pod updatedPod = new PodBuilder().withNewMetadata()
                .withName(podName)
                .addToAnnotations("test-reference", "12345")
                .addToLabels("key1", "value1").endMetadata()
                .build();

        updatedPod = kubernetesClient.pods().resource(updatedPod).createOr(NonDeletingOperation::update);
        return Response.ok(updatedPod).build();
    }

    @POST
    @Path("/{namespace}")
    public Response createNew(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.pods().inNamespace(namespace)
                        .resource(new PodBuilder().withNewMetadata()
                                .withName(UUID.randomUUID().toString())
                                .addToAnnotations("test-reference", "12345")
                                .endMetadata().build())
                        .create())
                .build();
    }

    @GET
    @Path("/{namespace}/{podName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("namespace") String namespace, @PathParam("podName") String name) {
        return Response.ok(kubernetesClient.pods().inNamespace(namespace).withName(name).get()).build();
    }

    @PUT
    @Path("/{namespace}/{podName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upsert(@PathParam("namespace") String namespace, @PathParam("podName") String name, Pod pod) {
        final Pod updatedPod = kubernetesClient.pods().inNamespace(namespace)
                .resource(new PodBuilder(pod).editMetadata().withName(name).endMetadata().build())
                .createOr(NonDeletingOperation::update);
        return Response.ok(updatedPod).build();
    }
}
