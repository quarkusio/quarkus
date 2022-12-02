package io.quarkus.grpc.server.scaling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

public class ScalingTestBase {

    Set<String> getThreadsUsedFor100Requests() throws InterruptedException, ExecutionException, TimeoutException {
        int requestNo = 100;
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < requestNo; i++) {
            calls.add(() -> {
                ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9001)
                        .usePlaintext()
                        .build();
                HelloReply reply = GreeterGrpc.newBlockingStub(channel)
                        .sayHello(HelloRequest.newBuilder().setName("foo").build());
                channel.shutdownNow();
                return reply.getMessage();
            });
        }
        List<Future<String>> results = Executors.newFixedThreadPool(requestNo)
                .invokeAll(calls);

        Set<String> threads = new HashSet<>();
        for (Future<String> result : results) {
            threads.add(result.get(10, TimeUnit.SECONDS));
        }
        return threads;
    }

}
