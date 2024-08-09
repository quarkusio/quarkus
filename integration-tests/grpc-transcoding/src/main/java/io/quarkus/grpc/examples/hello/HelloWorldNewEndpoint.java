package io.quarkus.grpc.examples.hello;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import examples.GreeterGrpc;
import examples.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.vertx.core.json.JsonObject;

@Path("/v2")
@Consumes("application/json")
@Produces("application/json")
public class HelloWorldNewEndpoint {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub service;

    @POST
    @Path("/simple")
    public String simplePath(JsonObject body) {
        String name = body.getString("name");
        return getJsonResponse(service.simplePath(HelloRequest.newBuilder().setName(name).build()));
    }

    @POST
    @Path("/complex/{name}/path")
    public String helloMutiny(@PathParam("name") String name) {
        return getJsonResponse(service.complexPath(HelloRequest.newBuilder().setName(name).build()));
    }

    private String getJsonResponse(Message message) {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
