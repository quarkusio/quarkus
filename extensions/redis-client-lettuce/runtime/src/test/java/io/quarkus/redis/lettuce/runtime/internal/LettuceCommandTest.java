package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisFuture;
import io.smallrye.mutiny.Uni;

class LettuceCommandTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @Test
    void toUniShouldApplyMapperToRawResult() {
        LettuceCommand<String, Integer> command = LettuceCommand.of(() -> completed("hello"), String::length);

        Integer result = command.toUni().await().atMost(TIMEOUT);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void toUniShouldBeLazy() {
        AtomicInteger callInvocations = new AtomicInteger();
        AtomicInteger mapperInvocations = new AtomicInteger();
        LettuceCommand<String, String> command = LettuceCommand.of(() -> {
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
        LettuceCommand<Integer, Integer> command = LettuceCommand.of(() -> completed(callInvocations.incrementAndGet()));

        Uni<Integer> uni = command.toUni();

        assertThat(uni.await().atMost(TIMEOUT)).isEqualTo(1);
        assertThat(uni.await().atMost(TIMEOUT)).isEqualTo(2);
        assertThat(callInvocations).hasValue(2);
    }

    @Test
    void toUniShouldPropagateFailedFutureWithoutInvokingMapper() {
        RuntimeException failure = new RuntimeException("boom");
        AtomicInteger mapperInvocations = new AtomicInteger();
        LettuceCommand<String, String> command = LettuceCommand.of(() -> failed(failure), raw -> {
            mapperInvocations.incrementAndGet();
            return raw;
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
        assertThat(mapperInvocations).hasValue(0);
    }

    @Test
    void toUniShouldPropagateExceptionThrownByCall() {
        RuntimeException failure = new IllegalStateException("cannot issue");
        LettuceCommand<String, String> command = LettuceCommand.of(() -> {
            throw failure;
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void toUniShouldPropagateExceptionThrownByMapper() {
        RuntimeException failure = new IllegalArgumentException("bad payload");
        LettuceCommand<String, String> command = LettuceCommand.of(() -> completed("raw"), raw -> {
            throw failure;
        });

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void ofWithoutMapperShouldReturnRawResult() {
        LettuceCommand<String, String> command = LettuceCommand.of(() -> completed("raw"));

        String result = command.toUni().await().atMost(TIMEOUT);

        assertThat(result).isEqualTo("raw");
    }

    @Test
    void staticDiscardingShouldInvokeCallAndReturnNull() {
        AtomicInteger callInvocations = new AtomicInteger();
        LettuceCommand<String, Void> command = LettuceCommand.discarding(() -> {
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
        LettuceCommand<String, Void> command = LettuceCommand.discarding(() -> failed(failure));

        assertThatThrownBy(() -> command.toUni().await().atMost(TIMEOUT)).isSameAs(failure);
    }

    @Test
    void instanceDiscardingShouldReuseCallAndDropMapper() {
        Supplier<RedisFuture<String>> call = () -> completed("hello");
        AtomicInteger mapperInvocations = new AtomicInteger();
        LettuceCommand<String, Integer> command = LettuceCommand.of(call, raw -> {
            mapperInvocations.incrementAndGet();
            return raw.length();
        });

        LettuceCommand<String, Void> discarded = command.discarding();

        assertThat(discarded.call()).isSameAs(call);
        assertThat(discarded.mapper()).isNotSameAs(command.mapper());

        Void result = discarded.toUni().await().atMost(TIMEOUT);

        assertThat(result).isNull();
        assertThat(mapperInvocations).hasValue(0);
    }

    @Test
    void instanceDiscardingShouldNotAlterOriginalCommand() {
        LettuceCommand<String, Integer> command = LettuceCommand.of(() -> completed("hello"), String::length);

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
    void failingShouldThrowWhenCallIsInvokedDirectly() {
        RuntimeException failure = new IllegalArgumentException("invalid argument");
        LettuceCommand<String, String> command = LettuceCommand.failing(failure);

        assertThatThrownBy(() -> command.call().get()).isSameAs(failure);
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
