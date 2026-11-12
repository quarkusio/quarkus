package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisFuture;
import io.smallrye.mutiny.Uni;

class LettuceCommandTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    /** The source of the commands under test: an established connection, whose command handle they ignore. */
    private static final LettuceConnection CONNECTION = FakeConnections.standalone();

    @Test
    void toUniShouldApplyMapperToRawResult() {
        LettuceCommand<String, Integer> command = LettuceCommand.of(CONNECTION, async -> completed("hello"), String::length);

        Integer result = command.toUni().await().atMost(TIMEOUT);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void toUniShouldBeLazy() {
        AtomicInteger callInvocations = new AtomicInteger();
        AtomicInteger mapperInvocations = new AtomicInteger();
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> {
            callInvocations.incrementAndGet();
            return completed("lazy");
        }, raw -> {
            mapperInvocations.incrementAndGet();
            return raw.toUpperCase();
        });

        Uni<String> uni = command.toUni();

        assertThat(callInvocations).hasValue(0);
        assertThat(mapperInvocations).hasValue(0);

        String result = uni.await().atMost(TIMEOUT);

        assertThat(result).isEqualTo("LAZY");
        assertThat(callInvocations).hasValue(1);
        assertThat(mapperInvocations).hasValue(1);
    }

    @Test
    void toUniShouldReissueCallOnEachSubscription() {
        AtomicInteger callInvocations = new AtomicInteger();
        LettuceCommand<Integer, Integer> command = LettuceCommand.of(CONNECTION,
                async -> completed(callInvocations.incrementAndGet()));

        Uni<Integer> uni = command.toUni();

        assertThat(uni.await().atMost(TIMEOUT)).isEqualTo(1);
        assertThat(uni.await().atMost(TIMEOUT)).isEqualTo(2);
        assertThat(callInvocations).hasValue(2);
    }

    @Test
    void toUniShouldPropagateFailedFutureWithoutInvokingMapper() {
        RuntimeException failure = new RuntimeException("boom");
        AtomicInteger mapperInvocations = new AtomicInteger();
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> failed(failure), raw -> {
            mapperInvocations.incrementAndGet();
            return raw;
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
        assertThat(mapperInvocations).hasValue(0);
    }

    @Test
    void toUniShouldPropagateExceptionThrownByCall() {
        RuntimeException failure = new IllegalStateException("cannot issue");
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> {
            throw failure;
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void toUniShouldPropagateExceptionThrownByMapper() {
        RuntimeException failure = new IllegalArgumentException("bad payload");
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> completed("raw"), raw -> {
            throw failure;
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void ofWithoutMapperShouldReturnRawResult() {
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> completed("raw"));

        String result = command.toUni().await().atMost(TIMEOUT);

        assertThat(result).isEqualTo("raw");
    }

    @Test
    void staticDiscardingShouldInvokeCallAndReturnNull() {
        AtomicInteger callInvocations = new AtomicInteger();
        LettuceCommand<String, Void> command = LettuceCommand.discarding(CONNECTION, async -> {
            callInvocations.incrementAndGet();
            return completed("ignored");
        });

        Void result = command.toUni().await().atMost(TIMEOUT);

        assertThat(result).isNull();
        assertThat(callInvocations).hasValue(1);
    }

    @Test
    void staticDiscardingShouldStillPropagateFailure() {
        RuntimeException failure = new RuntimeException("boom");
        LettuceCommand<String, Void> command = LettuceCommand.discarding(CONNECTION, async -> failed(failure));

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void instanceDiscardingShouldReuseCallAndDropMapper() {
        AtomicInteger callInvocations = new AtomicInteger();
        AtomicInteger mapperInvocations = new AtomicInteger();
        LettuceCommand<String, Integer> command = LettuceCommand.of(CONNECTION, async -> {
            callInvocations.incrementAndGet();
            return completed("hello");
        }, raw -> {
            mapperInvocations.incrementAndGet();
            return raw.length();
        });

        LettuceCommand<String, Void> discarded = command.discarding();

        assertThat(discarded.mapper()).isNotSameAs(command.mapper());

        Void result = discarded.toUni().await().atMost(TIMEOUT);

        assertThat(result).isNull();
        assertThat(callInvocations).hasValue(1);
        assertThat(mapperInvocations).hasValue(0);
    }

    @Test
    void rawShouldReuseCallAndDropMapper() {
        LettuceCommand<String, Integer> command = LettuceCommand.of(CONNECTION, async -> completed("hello"),
                String::length);

        assertThat(command.raw().toUni().await().atMost(TIMEOUT)).isEqualTo("hello");
        assertThat(command.toUni().await().atMost(TIMEOUT)).isEqualTo(5);
    }

    @Test
    void toUniShouldIssueTheCommandOnTheConnectionOfItsSource() {
        AtomicReference<Object> handle = new AtomicReference<>();
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> {
            handle.set(async);
            return completed("done");
        });

        command.toUni().await().atMost(TIMEOUT);

        assertThat(handle.get()).isSameAs(CONNECTION.async());
    }

    @Test
    void toUniShouldOpenASharedConnectionAndIssueTheCommandOnIt() {
        LettuceConnection opened = FakeConnections.standalone();
        AtomicInteger connects = new AtomicInteger();
        LettuceSharedConnection shared = new LettuceSharedConnection(() -> {
            connects.incrementAndGet();
            return CompletableFuture.completedFuture(opened);
        }, false);
        AtomicReference<Object> handle = new AtomicReference<>();
        LettuceCommand<String, String> command = LettuceCommand.of(shared, async -> {
            handle.set(async);
            return completed("done");
        });

        Uni<String> uni = command.toUni();
        assertThat(connects).hasValue(0);

        assertThat(uni.await().atMost(TIMEOUT)).isEqualTo("done");
        assertThat(uni.await().atMost(TIMEOUT)).isEqualTo("done");
        assertThat(handle.get()).isSameAs(opened.async());
        assertThat(connects).hasValue(1);
    }

    @Test
    void toUniShouldFailWhenTheSharedConnectionCannotBeOpened() {
        RuntimeException failure = new IllegalStateException("nobody listens");
        AtomicInteger callInvocations = new AtomicInteger();
        LettuceSharedConnection shared = new LettuceSharedConnection(() -> CompletableFuture.failedFuture(failure), false);
        LettuceCommand<String, String> command = LettuceCommand.of(shared, async -> {
            callInvocations.incrementAndGet();
            return completed("done");
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
        assertThat(callInvocations).hasValue(0);
    }

    @Test
    void issueOnShouldIssueTheCommandOnTheGivenConnectionAtOnce() {
        LettuceConnection other = FakeConnections.standalone();
        AtomicReference<Object> handle = new AtomicReference<>();
        LettuceCommand<String, String> command = LettuceCommand.of(CONNECTION, async -> {
            handle.set(async);
            return completed("done");
        });

        command.issueOn(other);

        assertThat(handle.get()).isSameAs(other.async());
    }

    @Test
    void onShouldBindTheCommandToAnotherSource() {
        LettuceConnection other = FakeConnections.standalone();
        AtomicReference<Object> handle = new AtomicReference<>();
        LettuceCommand<String, Integer> command = LettuceCommand.of(CONNECTION, async -> {
            handle.set(async);
            return completed("hello");
        }, String::length);

        assertThat(command.on(other).toUni().await().atMost(TIMEOUT)).isEqualTo(5);
        assertThat(handle.get()).isSameAs(other.async());

        command.toUni().await().atMost(TIMEOUT);
        assertThat(handle.get()).isSameAs(CONNECTION.async());
    }

    @Test
    void instanceDiscardingShouldNotAlterOriginalCommand() {
        LettuceCommand<String, Integer> command = LettuceCommand.of(CONNECTION, async -> completed("hello"), String::length);

        command.discarding();

        assertThat(command.toUni().await().atMost(TIMEOUT)).isEqualTo(5);
    }

    @Test
    void failingShouldFailUniWithGivenException() {
        RuntimeException failure = new IllegalArgumentException("invalid argument");
        LettuceCommand<String, String> command = LettuceCommand.failing(failure);

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void failingShouldThrowWhenIssuedOnAConnection() {
        RuntimeException failure = new IllegalArgumentException("invalid argument");
        LettuceCommand<String, String> command = LettuceCommand.failing(failure);

        assertThatThrownBy(() -> command.issueOn(CONNECTION)).isSameAs(failure);
        assertThatThrownBy(() -> command.discarding().issueOn(CONNECTION)).isSameAs(failure);
        assertThatThrownBy(() -> command.on(CONNECTION).toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void failingShouldNotThrowUntilSubscribed() {
        LettuceCommand<String, String> command = LettuceCommand.failing(new RuntimeException("deferred"));

        Uni<String> uni = command.toUni();

        assertThat(uni).isNotNull();
    }

    private static <T> RedisFuture<T> completed(T value) {
        TestRedisFuture<T> future = new TestRedisFuture<>();
        future.complete(value);
        return future;
    }

    private static <T> RedisFuture<T> failed(Throwable failure) {
        TestRedisFuture<T> future = new TestRedisFuture<>();
        future.completeExceptionally(failure);
        return future;
    }

    /**
     * Minimal {@link RedisFuture} backed by a {@link CompletableFuture}, so tests do not depend on
     * Lettuce's command/protocol internals.
     */
    private static final class TestRedisFuture<T> extends CompletableFuture<T> implements RedisFuture<T> {

        @Override
        public String getError() {
            return null;
        }

        @Override
        public boolean await(long timeout, TimeUnit unit) {
            return isDone();
        }
    }
}
