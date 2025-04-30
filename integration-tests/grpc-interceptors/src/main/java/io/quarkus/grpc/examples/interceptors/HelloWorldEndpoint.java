package io.quarkus.grpc.examples.interceptors;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;

@Path("/hello")
public class HelloWorldEndpoint {
    static Set<String> invoked = new HashSet<>();
    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyHelloService;

    @GET
    @Path("/blocking/{name}")
    public Response helloBlocking(@PathParam("name") String name) {
        invoked.clear();
        HelloReply helloReply = blockingHelloService.sayHello(HelloRequest.newBuilder().setName(name).build());

        return Response.ok(helloReply.getMessage())
                .header("interceptors", String.join(",", invoked))
                .header("used_cbc", MyCBC.USED.get() + "")
                .header("used_sbc", MySBC.USED.get() + "")
                .build();
    }

    @GET
    @Path("/mutiny/{name}")
    public Uni<String> helloMutiny(@PathParam("name") String name) {
        return mutinyHelloService.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(HelloReply::getMessage);
    }
}
