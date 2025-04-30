package io.quarkus.grpc.examples.hello;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;

@Path("/hello")
public class HelloWorldTlsEndpoint {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyHelloService;

    @GET
    @Path("/blocking/{name}")
    public String helloBlocking(@PathParam("name") String name) {
        return blockingHelloService.sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage();
    }

    @GET
    @Path("/mutiny/{name}")
    public Uni<String> helloMutiny(@PathParam("name") String name) {
        return mutinyHelloService.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(HelloReply::getMessage);
    }

    @GET
    @Path("/blocking-admin/{name}")
    public String roleAdminHelloBlocking(@PathParam("name") String name) {
        return blockingHelloService.sayHelloRoleAdmin(HelloRequest.newBuilder().setName(name).build()).getMessage();
    }

    @GET
    @Path("/mutiny-admin/{name}")
    public Uni<String> roleAdminHelloMutiny(@PathParam("name") String name) {
        return mutinyHelloService.sayHelloRoleAdmin(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(HelloReply::getMessage);
    }

    @GET
    @Path("/blocking-user/{name}")
    public String userRoleHelloBlocking(@PathParam("name") String name) {
        return blockingHelloService.sayHelloRoleUser(HelloRequest.newBuilder().setName(name).build()).getMessage();
    }

    @GET
    @Path("/mutiny-user/{name}")
    public Uni<String> userRoleHelloMutiny(@PathParam("name") String name) {
        return mutinyHelloService.sayHelloRoleUser(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(HelloReply::getMessage);
    }
}
