package io.quarkus.grpc.examples.hello;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;

@Path("/hello")
public class HelloWorldEndpoint {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyHelloService;

    @Inject
    HeaderCollectingInterceptor headerCollectingInterceptor;

    @GET
    @Path("/blocking/{name}")
    public String helloBlocking(@PathParam("name") String name) {
        HelloReply reply = blockingHelloService.sayHello(HelloRequest.newBuilder().setName(name).build());
        return generateResponse(reply);

    }

    @GET
    @Path("/mutiny/{name}")
    public Uni<String> helloMutiny(@PathParam("name") String name) {
        return mutinyHelloService.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform((reply) -> generateResponse(reply));
    }

    @Path("/encoding")
    @GET
    public String encodingUsedByGrpc() {
        return headerCollectingInterceptor.getEncoding();
    }

    @Path("/encoding")
    @DELETE
    public void deleteEncodingData() {
        headerCollectingInterceptor.clear();
    }

    public String generateResponse(HelloReply reply) {
        return String.format("%s! HelloWorldService has been called %d number of times.", reply.getMessage(), reply.getCount());
    }
}
