package io.quarkus.it.openshift.client;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;

@Path("/route")
public class Routes {

    // make sure we can inject both aspects of it
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    OpenShiftClient openshiftClient;

    @GET
    @Path("/{namespace}")
    public Response routes(@PathParam("namespace") String namespace) {
        if (kubernetesClient != openshiftClient) {
            return Response.serverError().entity("The clients are different").build();
        }
        return Response.ok(openshiftClient.routes().inNamespace(namespace).list().getItems()).build();
    }
}
