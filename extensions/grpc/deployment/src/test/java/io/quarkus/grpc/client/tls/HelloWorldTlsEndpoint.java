package io.quarkus.grpc.client.tls;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.smallrye.mutiny.Uni;

@Path("/hello")
public class HelloWorldTlsEndpoint {

    @Inject
    @GrpcService("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;
    @Inject
    @GrpcService("hello")
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
}
