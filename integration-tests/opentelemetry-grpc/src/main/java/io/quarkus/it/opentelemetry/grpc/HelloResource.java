package io.quarkus.it.opentelemetry.grpc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;

@Path("/grpc")
public class HelloResource {
    @GrpcClient
    Greeter hello;

    @GET
    @Path("/{name}")
    public Uni<String> hello(@PathParam("name") String name) {
        return hello.sayHello(HelloRequest.newBuilder().setName(name).build()).onItem().transform(HelloReply::getMessage);
    }
}
