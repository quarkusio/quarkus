package io.quarkus.grpc.examples.hello;

import static io.quarkus.grpc.examples.hello.IncomingInterceptor.EXTRA_BLOCKING_HEADER;
import static io.quarkus.grpc.examples.hello.IncomingInterceptor.EXTRA_HEADER;
import static io.quarkus.grpc.examples.hello.IncomingInterceptor.INTERFACE_HEADER;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import examples.Greeter;
import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import io.smallrye.mutiny.Uni;

@Path("/hello")
public class HelloWorldEndpoint {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloClient;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyHelloClient;

    @GrpcClient("hello")
    Greeter interfaceHelloClient;

    @Inject
    IncomingInterceptor interceptor;

    @GET
    @Path("/blocking/{name}")
    public String helloBlocking(@PathParam("name") String name, @QueryParam("headers") boolean headers) {
        Metadata extraHeaders = new Metadata();
        if (headers) {
            extraHeaders.put(EXTRA_BLOCKING_HEADER, "my-blocking-value");
        }
        HelloReply reply = GrpcClientUtils.attachHeaders(blockingHelloClient, extraHeaders)
                .sayHello(HelloRequest.newBuilder().setName(name).build());
        return generateResponse(reply);

    }

    @GET
    @Path("/mutiny/{name}")
    public Uni<String> helloMutiny(@PathParam("name") String name, @QueryParam("headers") boolean headers) {
        Metadata extraHeaders = new Metadata();
        if (headers) {
            extraHeaders.put(EXTRA_HEADER, "my-extra-value");
        }
        MutinyGreeterGrpc.MutinyGreeterStub alteredClient = GrpcClientUtils.attachHeaders(mutinyHelloClient, extraHeaders);
        return alteredClient.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(this::generateResponse);
    }

    @GET
    @Path("/interface/{name}")
    public Uni<String> helloInterface(@PathParam("name") String name, @QueryParam("headers") boolean headers) {
        Metadata extraHeaders = new Metadata();
        if (headers) {
            extraHeaders.put(INTERFACE_HEADER, "my-interface-value");
        }

        Greeter alteredClient = GrpcClientUtils.attachHeaders(interfaceHelloClient, extraHeaders);

        return alteredClient.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(this::generateResponse);

    }

    @DELETE
    public void clear() {
        interceptor.clear();
    }

    @GET
    @Path("/headers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getCollectedHeaders() {
        return interceptor.getCollectedHeaders();
    }

    public String generateResponse(HelloReply reply) {
        return String.format("%s! HelloWorldService has been called %d number of times.", reply.getMessage(), reply.getCount());
    }
}
