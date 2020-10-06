package io.quarkus.it.openshift.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.fabric8.openshift.client.OpenShiftClient;

@Path("/route")
public class Routes {

    private final OpenShiftClient openshiftClient;

    public Routes(OpenShiftClient openshiftClient) {
        this.openshiftClient = openshiftClient;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{namespace}")
    public Response routes(@PathParam("namespace") String namespace) {
        return Response.ok(openshiftClient.routes().inNamespace(namespace).list().getItems()).build();
    }
}
