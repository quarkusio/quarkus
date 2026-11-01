package io.quarkus.grpc.context;

import java.time.Duration;

import io.grpc.Context;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.context.proto.CheckReply;
import io.quarkus.grpc.context.proto.CheckRequest;
import io.quarkus.grpc.context.proto.MutinyContextCheckGrpc;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

@GrpcService
public class ContextCheckService extends MutinyContextCheckGrpc.ContextCheckImplBase {

    @Override
    public Uni<CheckReply> check(CheckRequest request) {
        return Uni.createFrom().item(contextReply());
    }

    @Blocking
    @Override
    public Uni<CheckReply> checkBlocking(CheckRequest request) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Uni.createFrom().item(contextReply());
    }

    @RunOnVirtualThread
    @Override
    public Uni<CheckReply> checkVirtual(CheckRequest request) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Uni.createFrom().item(contextReply());
    }

    @Blocking
    @Override
    public Uni<CheckReply> checkSlowBlocking(CheckRequest request) {
        return slowReply();
    }

    @RunOnVirtualThread
    @Override
    public Uni<CheckReply> checkSlowVirtual(CheckRequest request) {
        return slowReply();
    }

    private static Uni<CheckReply> slowReply() {
        // Hold the call open for 2 s without blocking any thread. When the client
        // cancels (deadline), Mutiny cancels the timer subscription before it fires,
        // so no response is ever sent and "Headers already sent" is avoided.
        return Uni.createFrom().item(ContextCheckService::contextReply)
                .onItem().delayIt().by(Duration.ofSeconds(2));
    }

    private static CheckReply contextReply() {
        return CheckReply.newBuilder()
                .setContextState(Context.current() != Context.ROOT ? "ok" : "root")
                .build();
    }

}
