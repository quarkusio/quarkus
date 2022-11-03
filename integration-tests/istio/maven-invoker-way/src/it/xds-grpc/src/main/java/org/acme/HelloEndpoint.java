package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hello")
public class HelloEndpoint {
    private static final Logger log = LoggerFactory.getLogger(HelloEndpoint.class);

    @GrpcClient
    GreeterGrpc.GreeterBlockingStub stub;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        HelloRequest request = HelloRequest.newBuilder().setName("XDS gRPC").build();
        HelloReply response;
        try {
            response = stub.sayHello(request);
        } catch (StatusRuntimeException e) {
            String msg = "RPC failed: " + e.getStatus();
            log.warn(msg);
            return msg;
        }
        return response.getMessage();
    }
}