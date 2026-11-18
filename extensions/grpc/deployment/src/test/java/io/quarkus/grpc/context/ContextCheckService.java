package io.quarkus.grpc.context;

import java.time.Duration;

import io.grpc.Context;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.context.proto.CheckReply;
import io.quarkus.grpc.context.proto.CheckRequest;
import io.quarkus.grpc.context.proto.MutinyContextCheckGrpc;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class ContextCheckService extends MutinyContextCheckGrpc.ContextCheckImplBase {

    @GrpcClient("context-check")
    MutinyContextCheckGrpc.MutinyContextCheckStub nestedClient;

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

    @Override
    public Uni<CheckReply> checkStreaming(Multi<CheckRequest> request) {
        return request.collect().last().map(last -> contextReply());
    }

    @Blocking
    @Override
    public Uni<CheckReply> checkStreamingBlocking(Multi<CheckRequest> request) {
        return request.collect().last().map(last -> contextReply());
    }

    @RunOnVirtualThread
    @Override
    public Uni<CheckReply> checkStreamingVirtual(Multi<CheckRequest> request) {
        return request.collect().last().map(last -> contextReply());
    }

    @Override
    public Uni<CheckReply> checkSlowEventLoopResponding(CheckRequest request) {
        return slowReply();
    }

    @Override
    public Uni<CheckReply> checkSlowEventLoop(CheckRequest request) {
        // Never emits: ensures the service does not respond before the client
        // cancels, so onCancel() fires rather than onComplete().
        return Uni.createFrom().nothing();
    }

    @Override
    public Uni<CheckReply> checkNested(CheckRequest request) {
        Context outerCtx = Context.current();
        String outerBefore = outerCtx != Context.ROOT ? "ok" : "root";
        return nestedClient.check(CheckRequest.getDefaultInstance())
                .map(innerReply -> {
                    String outerAfter = Context.current() != Context.ROOT ? "ok" : "root";
                    String sameCtx = Context.current() == outerCtx ? "same" : "different";
                    return CheckReply.newBuilder()
                            .setContextState(
                                    outerBefore + "," + innerReply.getContextState() + "," + outerAfter + "," + sameCtx)
                            .build();
                });
    }

    private static Uni<CheckReply> slowReply() {
        // Hold the call open for 2 s without blocking any thread.
        return Uni.createFrom().item(ContextCheckService::contextReply)
                .onItem().delayIt().by(Duration.ofSeconds(2));
    }

    private static CheckReply contextReply() {
        return CheckReply.newBuilder()
                .setContextState(Context.current() != Context.ROOT ? "ok" : "root")
                .build();
    }

}
