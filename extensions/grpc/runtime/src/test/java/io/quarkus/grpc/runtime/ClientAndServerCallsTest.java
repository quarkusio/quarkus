package io.quarkus.grpc.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ClientAndServerCallsTest {

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);
    private FakeServiceClient client = new FakeServiceClient();

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
                .getCause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.UNKNOWN.getCode());
                    assertThat((t).getMessage()).contains("boom").contains("Exception");
                });

        assertThatThrownBy(() -> client.immediateFailure("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .getCause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.UNKNOWN.getCode());
                    assertThat((t).getMessage()).contains("runtime boom").contains("RuntimeException");
                });

        assertThatThrownBy(() -> client.illegalArgumentException("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .getCause().satisfies(t -> {
                    assertThat(t).isInstanceOf(StatusException.class);
                    assertThat(((StatusException) t).getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
                    assertThat((t).getMessage()).contains("bad").contains("IllegalArgumentException");
                });

        assertThatThrownBy(() -> client.npe("hello").await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(StatusException.class)
                .getCause().satisfies(t -> {
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
            return ClientCalls.manyToOne(multi, o -> ServerCalls.manyToOne(o, service::manyToOne));
        }

        Multi<String> oneToMany(String s) {
            return ClientCalls.oneToMany(s, (i, o) -> ServerCalls.oneToMany(i, o, null, service::oneToMany));
        }

        Multi<String> manyToMany(Multi<String> multi) {
            return ClientCalls.manyToMany(multi, o -> ServerCalls.manyToMany(o, service::manyToMany));
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
