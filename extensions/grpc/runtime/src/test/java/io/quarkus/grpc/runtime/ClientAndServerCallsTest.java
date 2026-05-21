package io.quarkus.grpc.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.quarkus.grpc.stubs.ClientCalls;
import io.quarkus.grpc.stubs.ServerCalls;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ClientAndServerCallsTest {

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);
    private FakeServiceClient client = new FakeServiceClient();

    private static class EHP implements ExceptionHandlerProvider {
        @Override
        public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
                ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            return new ExceptionHandler<>(listener, serverCall, metadata) {
                @Override
                protected void handleException(Throwable t, ServerCall<ReqT, RespT> call, Metadata metadata) {
                    Status status = ExceptionHandlerProvider.toStatus(t);
                    Optional<Metadata> trailers = ExceptionHandlerProvider.toTrailers(t);
                    call.close(status, trailers.orElse(metadata));
                }
            };
        }

        @Override
        public Throwable transform(Throwable t) {
            return ExceptionHandlerProvider.toStatusException(t, false);
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        Field ehp = ServerCalls.class.getDeclaredField("ehp");
        ehp.setAccessible(true);
        ehp.set(null, new EHP());
    }

    @Test
    public void oneToOneSuccess() {
        Uni<String> result = ClientCalls.oneToOne("hello", (i, o) -> {
            o.onNext(i);
            o.onCompleted();
        });
        assertThat(result.await().atMost(TIMEOUT)).isEqualTo("hello");
    }

    @Test
    public void oneToOneFailure() {
        Uni<String> result = ClientCalls.oneToOne("hello", (i, o) -> o.onError(new IOException("boom")));
        assertThatThrownBy(() -> result.await().atMost(TIMEOUT)).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    public void oneToOneFailureAfterEmission() {
        Uni<String> result = ClientCalls.oneToOne("hello", (i, o) -> {
            o.onNext(i);
            o.onError(new IOException("too late"));
        });
        assertThat(result.await().atMost(TIMEOUT)).isEqualTo("hello");
    }

    @Test
    public void testOneToOne() {
        assertThat(client.oneToOne("hello").await().atMost(TIMEOUT)).isEqualTo("HELLO");
    }

    @Test
    public void testOneToMany() {
        assertThat(client.oneToMany("hello").collect().asList().await().atMost(TIMEOUT)).containsExactly("HELLO", "HELLO");
    }

    @Test
    public void testManyToOne() {
        assertThat(client.manyToOne(Multi.createFrom().items("hello", "world")).await().atMost(TIMEOUT))
                .containsExactly("HELLO", "WORLD");
    }

    @Test
    public void testManyToMany() {
        assertThat(client.manyToMany(Multi.createFrom().items("hello", "world"))
                .collect().asList()
                .await().atMost(TIMEOUT)).containsExactly("HELLO", "WORLD");
    }

    @Test
    public void testFailureReporting() {
        FailingServiceClient client = new FailingServiceClient();
        assertThatThrownBy(() -> client.propagateFailure("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .cause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.UNKNOWN.getCode());
                    assertThat((t).getMessage()).contains("boom").contains("Exception");
                });

        assertThatThrownBy(() -> client.immediateFailure("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .cause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.UNKNOWN.getCode());
                    assertThat((t).getMessage()).contains("runtime boom").contains("RuntimeException");
                });

        assertThatThrownBy(() -> client.illegalArgumentException("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .cause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
                    assertThat((t).getMessage()).contains("bad").contains("IllegalArgumentException");
                });

        assertThatThrownBy(() -> client.npe("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .cause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.UNKNOWN.getCode());
                    assertThat((t).getMessage()).contains("NullPointerException");
                });
    }

    @Test
    public void testOneToManyFlowControl() {
        var client2 = new FakeServiceClient(false, new FakeServiceVolume());
        AtomicInteger received = new AtomicInteger();
        client2.oneToMany("hello").subscribe().with(item -> received.getAndIncrement());

        // Not ready — upstream prefetched PREFETCH items but nothing drained yet
        assertEquals(0, received.get());
        assertEquals(1, client2.service.requests.size());
        assertThat(client2.service.requests.get(0)).isEqualTo(256L);

        // Drain 256 items then pause — triggers 1 replenish at consumed=192
        client2.fakeServerCallStreamObserver.flipReadyAfterIterations = 256;
        client2.fakeServerCallStreamObserver.setReady(true);
        assertEquals(256, received.get());
        assertEquals(2, client2.service.requests.size());
        assertThat(client2.service.requests.get(1)).isEqualTo(192L);

        // Drain 128 more — consumed goes 64→192, triggers another replenish
        client2.fakeServerCallStreamObserver.flipReadyAfterIterations = 128;
        client2.fakeServerCallStreamObserver.setReady(true);
        assertEquals(384, received.get());
        assertEquals(3, client2.service.requests.size());
        assertThat(client2.service.requests.get(2)).isEqualTo(192L);

        // Drain just 1 item — consumed=1, well below replenish threshold
        client2.fakeServerCallStreamObserver.flipReadyAfterIterations = 1;
        client2.fakeServerCallStreamObserver.setReady(true);
        assertEquals(385, received.get());
        assertEquals(3, client2.service.requests.size());
    }

    @Test
    public void testManyToOneFlowControl() {
        var cf = new CompletableFuture<List<String>>();
        var client2 = new FakeServiceClient(false, new FakeServiceVolume());
        var requests = new ArrayList<Long>();
        var res = client2.manyToOne(
                Multi.createFrom()
                        .range(0, 1000).map(Object::toString)
                        .onRequest().invoke(requests::add))
                .subscribe().with(cf::complete);

        // Initial prefetch — drain loop requested PREFETCH from upstream, nothing forwarded yet
        assertEquals(1, requests.size());
        assertThat(requests.get(0)).isEqualTo(256L);

        // Drain 256 items — triggers 1 replenish at consumed=192
        client2.fakeClientCallStreamObserver.flipReadyAfterIterations = 256;
        client2.fakeClientCallStreamObserver.setReady(true);
        assertEquals(2, requests.size());
        assertThat(requests.get(1)).isEqualTo(192L);

        // Drain 128 more — consumed goes 64→192, triggers another replenish
        client2.fakeClientCallStreamObserver.flipReadyAfterIterations = 128;
        client2.fakeClientCallStreamObserver.setReady(true);
        assertEquals(3, requests.size());
        assertThat(requests.get(2)).isEqualTo(192L);

        // Drain just 1 item — consumed=1, well below replenish threshold
        client2.fakeClientCallStreamObserver.flipReadyAfterIterations = 1;
        client2.fakeClientCallStreamObserver.setReady(true);
        assertEquals(3, requests.size());
    }

    @Test
    public void testManyToManyFlowControl() {
        var client2 = new FakeServiceClient(false, new FakeService());
        var inputRequests = new ArrayList<Long>();
        AtomicInteger received = new AtomicInteger();

        client2.manyToMany(
                Multi.createFrom().range(0, 1000).map(Object::toString)
                        .onRequest().invoke(inputRequests::add))
                .subscribe().with(item -> received.getAndIncrement());

        // Both sides not ready — client prefetched from input Multi, nothing forwarded
        assertEquals(0, received.get());
        assertEquals(1, inputRequests.size());
        assertThat(inputRequests.get(0)).isEqualTo(256L);

        // Make client ready for 256 — items flow to server queue but server not ready
        client2.fakeClientCallStreamObserver.flipReadyAfterIterations = 256;
        client2.fakeClientCallStreamObserver.setReady(true);
        assertEquals(0, received.get());
        assertEquals(2, inputRequests.size());
        assertThat(inputRequests.get(1)).isEqualTo(192L);

        // Make server ready for 256 — items drain to downstream subscriber
        client2.fakeServerCallStreamObserver.flipReadyAfterIterations = 256;
        client2.fakeServerCallStreamObserver.setReady(true);
        assertEquals(256, received.get());
    }

    static class FakeService {

        List<Long> requests = new ArrayList<>();

        Uni<String> oneToOne(String s) {
            return Uni.createFrom().item(s).map(String::toUpperCase);
        }

        Uni<List<String>> manyToOne(Multi<String> multi) {
            return multi.map(String::toUpperCase).collect().asList();
        }

        Multi<String> oneToMany(String s) {
            return Multi.createFrom().items(s, s).map(String::toUpperCase);
        }

        Multi<String> manyToMany(Multi<String> multi) {
            return multi.map(String::toUpperCase);
        }

    }

    static class FakeServiceVolume extends FakeService {

        Multi<String> oneToMany(String s) {
            return Multi.createFrom()
                    .range(0, 1000)
                    .map(i -> s.toUpperCase())
                    .onRequest()
                    .invoke(r -> requests.add(r));
        }
    }

    static class FakeServiceClient {

        FakeService service = new FakeService();
        boolean startReady = true;

        public FakeClientCallStreamObserver<String> fakeClientCallStreamObserver;
        public FakeServerCallStreamObserver<String> fakeServerCallStreamObserver;

        public FakeServiceClient() {
        }

        public FakeServiceClient(boolean startReady, FakeService service) {
            this.service = service;
            this.startReady = startReady;
        }

        Uni<String> oneToOne(String s) {
            return ClientCalls.oneToOne(s, (i, o) -> ServerCalls.oneToOne(i, o, null, service::oneToOne));
        }

        Uni<List<String>> manyToOne(Multi<String> multi) {
            return ClientCalls.manyToOne(multi,
                    o -> {
                        fakeClientCallStreamObserver = new FakeClientCallStreamObserver<>(
                                ServerCalls.manyToOne(o, service::manyToOne),
                                startReady);
                        ((ClientResponseObserver<String, List<String>>) o).beforeStart(fakeClientCallStreamObserver);
                        return fakeClientCallStreamObserver;
                    });
        }

        Multi<String> oneToMany(String s) {
            return ClientCalls.oneToMany(s,
                    (i, o) -> {
                        fakeServerCallStreamObserver = new FakeServerCallStreamObserver<>(o, startReady);
                        ServerCalls.oneToMany(i, fakeServerCallStreamObserver, null, service::oneToMany);
                    });
        }

        Multi<String> manyToMany(Multi<String> multi) {
            return ClientCalls.manyToMany(multi, o -> {
                fakeServerCallStreamObserver = new FakeServerCallStreamObserver<String>(o, startReady);
                fakeClientCallStreamObserver = new FakeClientCallStreamObserver<String>(
                        ServerCalls.manyToMany(fakeServerCallStreamObserver, service::manyToMany),
                        startReady);
                ((ClientResponseObserver<String, String>) o).beforeStart(fakeClientCallStreamObserver);
                return fakeClientCallStreamObserver;
            });
        }
    }

    static class FakeServerCallStreamObserver<T> extends ServerCallStreamObserver<T> {
        private final StreamObserver<T> delegate;
        private boolean isReady = true;
        private Runnable onReadyHandler;
        int flipReadyAfterIterations = 5;
        int currentIteration = 0;

        FakeServerCallStreamObserver(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        FakeServerCallStreamObserver(StreamObserver<T> delegate, boolean isReady) {
            this.delegate = delegate;
            this.isReady = isReady;
        }

        @Override
        public void onNext(T value) {
            if (currentIteration == flipReadyAfterIterations - 1) {
                setReady(false);
            }
            delegate.onNext(value);
            currentIteration++;
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
        }

        @Override
        public void setCompression(String compression) {
        }

        @Override
        public boolean isReady() {
            return isReady;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
            this.onReadyHandler = onReadyHandler;
        }

        @Override
        public void request(int i) {
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        @Override
        public void setMessageCompression(boolean enable) {
        }

        @Override
        public void setOnCloseHandler(Runnable onCloseHandler) {
        }

        public void setReady(boolean isReady) {
            this.isReady = isReady;
            currentIteration = 0;
            if (this.isReady) {
                this.onReadyHandler.run();
            }
        }
    }

    static class FakeClientCallStreamObserver<T> extends ClientCallStreamObserver<T> {
        private final StreamObserver<T> delegate;

        private boolean isReady = true;
        private Runnable onReadyHandler;
        private List<Integer> requests = new ArrayList<Integer>();
        int flipReadyAfterIterations = 5;
        int currentIteration = 0;

        FakeClientCallStreamObserver(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        FakeClientCallStreamObserver(StreamObserver<T> delegate, boolean isReady) {
            this.delegate = delegate;
            this.isReady = isReady;
        }

        @Override
        public void onNext(T value) {
            if (currentIteration == flipReadyAfterIterations - 1) {
                setReady(false);
            }
            delegate.onNext(value);
            currentIteration++;
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }

        @Override
        public boolean isReady() {
            return isReady;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
            this.onReadyHandler = onReadyHandler;
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        public void setReady(boolean isReady) {
            this.isReady = isReady;
            if (this.isReady) {
                currentIteration = 0;
                onReadyHandler.run();
            }
        }

        @Override
        public void request(int count) {
            this.requests.add(count);
        }

        @Override
        public void setMessageCompression(boolean enable) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
        }
    }

    static class FailingService {

        Uni<String> propagateFailure(String s) {
            return Uni.createFrom().failure(new Exception("boom"));
        }

        Uni<String> immediateFailure(String s) {
            throw new RuntimeException("runtime boom");
        }

        Uni<String> illegalArgumentException(String s) {
            throw new IllegalArgumentException("bad");
        }

        Uni<String> npe(String s) {
            throw new NullPointerException();
        }

    }

    static class FailingServiceClient {

        FailingService service = new FailingService();

        Uni<String> propagateFailure(String s) {
            return ClientCalls.oneToOne(s, (i, o) -> ServerCalls.oneToOne(i, o, null, service::propagateFailure));
        }

        Uni<String> immediateFailure(String s) {
            return ClientCalls.oneToOne(s, (i, o) -> ServerCalls.oneToOne(i, o, null, service::immediateFailure));
        }

        Uni<String> illegalArgumentException(String s) {
            return ClientCalls.oneToOne(s, (i, o) -> ServerCalls.oneToOne(i, o, null, service::illegalArgumentException));
        }

        Uni<String> npe(String s) {
            return ClientCalls.oneToOne(s, (i, o) -> ServerCalls.oneToOne(i, o, null, service::npe));
        }

    }

}
