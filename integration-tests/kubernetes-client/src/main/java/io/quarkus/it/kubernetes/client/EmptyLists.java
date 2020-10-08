package io.quarkus.it.kubernetes.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.client.KubernetesClient;

@Produces(MediaType.APPLICATION_JSON)
@Path("empty/{namespace}")
public class EmptyLists {

    private final KubernetesClient kubernetesClient;

    public EmptyLists(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @GET
    @Path("configmaps")
    public Response configmaps(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.configMaps().list().getItems())
                .build();
    }

    @GET
    @Path("deployments")
    public Response deployments(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.apps().deployments().list().getItems())
                .build();
    }

    @GET
    @Path("events")
    public Response events(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.v1().events().list().getItems())
                .build();
    }

    @GET
    @Path("ingresses")
    public Response ingresses(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.extensions().ingresses().list().getItems())
                .build();
    }

    @GET
    @Path("pods")
    public Response pods(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.pods().list().getItems())
                .build();
    }

    @GET
    @Path("secrets")
    public Response secrets(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.secrets().list().getItems())
                .build();
    }

    @GET
    @Path("services")
    public Response services(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.services().list().getItems())
                .build();
    }

    @GET
    @Path("serviceaccounts")
    public Response serviceaccounts(@PathParam("namespace") String namespace) {
        return Response
                .ok(kubernetesClient.serviceAccounts().list().getItems())
                .build();
    }

}
