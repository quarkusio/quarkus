package org.acme;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloGrpcService implements HelloGrpc {
    private final LongCounter counter;

    public HelloGrpcService(Meter meter) {
        counter = meter.counterBuilder("hello-metrics")
                .setDescription("hello-metrics")
                .setUnit("invocations")
                .build();
    }

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        counter.add(1);

        return Uni.createFrom().item("Hello " + request.getName() + "!")
                .map(msg -> HelloReply.newBuilder().setMessage(msg).build());
    }
}
