package io.quarkus.grpc.lock.detection;

import org.jboss.logging.Logger;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.blocking.call.test.CallBlocking;
import io.quarkus.grpc.blocking.call.test.CallHello;
import io.quarkus.grpc.blocking.call.test.CallHello.SuccessOrFailureDescription;
import io.smallrye.mutiny.Uni;

@GrpcService
public class CallBlockingService implements CallBlocking {

    public static String EXPECTED_ERROR_PREFIX = "Blocking gRPC client call made from the event loop";

    private static final Logger log = Logger.getLogger(CallBlockingService.class);

    @GrpcClient
    GreeterGrpc.GreeterBlockingStub blockingClient;

    @Override
    public Uni<SuccessOrFailureDescription> doBlockingCall(CallHello.Empty request) {
        try {
            HelloReply reply = blockingClient.sayHello(HelloRequest.newBuilder().setName("Bob the Blocker").build());
            return Uni.createFrom().item(reply)
                    .map(helloReply -> SuccessOrFailureDescription.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            if (!e.getMessage().contains(EXPECTED_ERROR_PREFIX)) {
                log.error("Error ", e);
            }
            return Uni.createFrom().item(SuccessOrFailureDescription.newBuilder().setSuccess(false)
                    .setErrorDescription(e.getMessage()).build());
        }
    }
}
