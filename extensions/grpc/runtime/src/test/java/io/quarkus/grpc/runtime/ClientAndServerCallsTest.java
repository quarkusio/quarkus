package io.quarkus.grpc.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.ClientCallStreamObserver;
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

    static class FakeService {

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

    static class FakeServiceClient {

        FakeService service = new FakeService();

        Uni<String> oneToOne(String s) {
            return ClientCalls.oneToOne(s, (i, o) -> ServerCalls.oneToOne(i, o, null, service::oneToOne));
        }

        Uni<List<String>> manyToOne(Multi<String> multi) {
            return ClientCalls.manyToOne(multi,
                    o -> new FakeClientCallStreamObserver<>(ServerCalls.manyToOne(o, service::manyToOne)));
        }

        Multi<String> oneToMany(String s) {
            return ClientCalls.oneToMany(s,
                    (i, o) -> ServerCalls.oneToMany(i, new FakeServerCallStreamObserver<>(o), null, service::oneToMany));
        }

        Multi<String> manyToMany(Multi<String> multi) {
            return ClientCalls.manyToMany(multi, o -> new FakeClientCallStreamObserver<>(
                    ServerCalls.manyToMany(new FakeServerCallStreamObserver<>(o), service::manyToMany)));
        }
    }

    static class FakeServerCallStreamObserver<T> extends ServerCallStreamObserver<T> {
        private final StreamObserver<T> delegate;

        FakeServerCallStreamObserver(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
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
            return true;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        @Override
        public void request(int count) {
        }

        @Override
        public void setMessageCompression(boolean enable) {
        }

        @Override
        public void setOnCloseHandler(Runnable onCloseHandler) {
        }
    }

    static class FakeClientCallStreamObserver<T> extends ClientCallStreamObserver<T> {
        private final StreamObserver<T> delegate;

        FakeClientCallStreamObserver(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
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
            return true;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        @Override
        public void request(int count) {
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
