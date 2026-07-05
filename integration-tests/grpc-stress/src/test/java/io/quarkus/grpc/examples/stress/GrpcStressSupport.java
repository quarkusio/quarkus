package io.quarkus.grpc.examples.stress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

final class GrpcStressSupport {

    static final int CONCURRENT_THREADS = 32;
    static final int CALLS_PER_THREAD = 64;
    static final int TOTAL_UNARY_CALLS = CONCURRENT_THREADS * CALLS_PER_THREAD;
    static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    static final int LARGE_PAYLOAD_BYTES = 1024 * 1024;
    static final int LARGE_PAYLOAD_CALLS = 20;
    static final int STREAM_MESSAGES_PER_CALL = 50;
    static final int CONCURRENT_STREAMS = 32;

    private GrpcStressSupport() {
    }

    static void runConcurrentUnaryEcho(StressGrpc.StressBlockingStub stub) throws InterruptedException {
        Set<Long> received = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        try {
            for (int thread = 0; thread < CONCURRENT_THREADS; thread++) {
                final int threadIndex = thread;
                executor.submit(() -> {
                    try {
                        for (int call = 0; call < CALLS_PER_THREAD; call++) {
                            long id = ((long) threadIndex * CALLS_PER_THREAD) + call;
                            try {
                                IdReply reply = stub.echoId(IdRequest.newBuilder().setId(id).build());
                                if (!received.add(reply.getId())) {
                                    failures.incrementAndGet();
                                }
                            } catch (StatusRuntimeException e) {
                                failures.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(2, TimeUnit.MINUTES)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures).hasValue(0);
        assertThat(received).hasSize(TOTAL_UNARY_CALLS);
        assertThat(received).containsExactlyInAnyOrderElementsOf(expectedIds());
    }

    static void runConcurrentUnaryEcho(MutinyStressGrpc.MutinyStressStub stub) {
        List<Uni<IdReply>> calls = new ArrayList<>(TOTAL_UNARY_CALLS);
        for (long id = 0; id < TOTAL_UNARY_CALLS; id++) {
            long requestId = id;
            calls.add(stub.echoId(IdRequest.newBuilder().setId(requestId).build()));
        }

        List<IdReply> replies = Uni.combine().all().unis(calls).with(list -> {
            @SuppressWarnings("unchecked")
            List<IdReply> typed = (List<IdReply>) list;
            return typed;
        }).await().atMost(CALL_TIMEOUT);

        Set<Long> received = ConcurrentHashMap.newKeySet();
        for (IdReply reply : replies) {
            assertThat(received.add(reply.getId())).isTrue();
        }
        assertThat(received).hasSize(TOTAL_UNARY_CALLS);
        assertThat(received).containsExactlyInAnyOrderElementsOf(expectedIds());
    }

    static void runLargePayloadEcho(StressGrpc.StressBlockingStub stub, byte[] payload) {
        for (int i = 0; i < LARGE_PAYLOAD_CALLS; i++) {
            PayloadReply reply = stub.echoPayload(PayloadRequest.newBuilder()
                    .setId(i)
                    .setData(com.google.protobuf.ByteString.copyFrom(payload))
                    .build());
            assertThat(reply.getId()).isEqualTo(i);
            assertThat(reply.getSize()).isEqualTo(payload.length);
        }
    }

    static void runConcurrentClientStreaming(Channel channel) throws InterruptedException {
        Set<Long> received = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_STREAMS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_STREAMS);

        try {
            for (int stream = 0; stream < CONCURRENT_STREAMS; stream++) {
                final int streamIndex = stream;
                executor.submit(() -> {
                    try {
                        MutinyStressGrpc.MutinyStressStub stub = MutinyStressGrpc.newMutinyStub(channel);
                        List<IdRequest> requests = new ArrayList<>(STREAM_MESSAGES_PER_CALL);
                        for (int message = 0; message < STREAM_MESSAGES_PER_CALL; message++) {
                            long id = ((long) streamIndex * STREAM_MESSAGES_PER_CALL) + message;
                            requests.add(IdRequest.newBuilder().setId(id).build());
                        }

                        List<IdReply> replies = stub.streamIds(Multi.createFrom().iterable(requests))
                                .collect().asList()
                                .await().atMost(CALL_TIMEOUT);

                        for (IdReply reply : replies) {
                            if (!received.add(reply.getId())) {
                                failures.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(2, TimeUnit.MINUTES)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures).hasValue(0);
        assertThat(received).hasSize(CONCURRENT_STREAMS * STREAM_MESSAGES_PER_CALL);
    }

    static byte[] oneMegabytePayload() {
        return new byte[LARGE_PAYLOAD_BYTES];
    }

    private static List<Long> expectedIds() {
        List<Long> ids = new ArrayList<>(TOTAL_UNARY_CALLS);
        for (long id = 0; id < TOTAL_UNARY_CALLS; id++) {
            ids.add(id);
        }
        return Collections.unmodifiableList(ids);
    }
}
