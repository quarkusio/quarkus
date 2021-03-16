package io.quarkus.grpc.examples.interceptors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.smallrye.mutiny.Uni;

@Path("/hello")
public class HelloWorldEndpoint {

    @Inject
    @GrpcService("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;
    @Inject
    @GrpcService("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyHelloService;

    @GET
    @Path("/blocking/{name}")
    public Response helloBlocking(@PathParam("name") String name) {
        HeaderClientInterceptor.invoked = false;
        HelloReply helloReply = blockingHelloService.sayHello(HelloRequest.newBuilder().setName(name).build());

        return Response.ok(helloReply.getMessage()).header("intercepted", HeaderClientInterceptor.invoked).build();
    }

    @GET
    @Path("/mutiny/{name}")
    public Uni<String> helloMutiny(@PathParam("name") String name) {
        return mutinyHelloService.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(HelloReply::getMessage);
    }
}
