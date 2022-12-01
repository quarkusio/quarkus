package io.quarkus.grpc.examples.hello;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import examples.Greeter;
import examples.HelloReply;
import examples.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;

@Path("/test")
public class GrpcCallingResource {

    @GrpcClient("hello1")
    Greeter client1;
    @GrpcClient("hello2")
    Greeter client2;

    @GET
    @Path("/unary/1")
    public Uni<String> unaryCall1(@QueryParam("headers") boolean headers) {
        return client1.sayHello(HelloRequest.newBuilder().setName("World").build())
                .onItem().transform(HelloReply::getMessage);
    }

    @GET
    @Path("/unary/2")
    public Uni<String> unaryCall2(@QueryParam("headers") boolean headers) {
        return client2.sayHello(HelloRequest.newBuilder().setName("World").build())
                .onItem().transform(HelloReply::getMessage);
    }

    @POST
    @Path("/delay")
    public String setDelay(Integer delayMs) {
        GrpcServices.delayMs = delayMs;
        return "done";
    }
}
