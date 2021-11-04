package io.quarkus.reactivemessaging.http.sink;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.sink.app.Dto;
import io.quarkus.reactivemessaging.http.sink.app.HttpEmitterWithOverflow;
import io.quarkus.reactivemessaging.http.sink.app.HttpEndpoint;
import io.quarkus.reactivemessaging.utils.ToUpperCaseSerializer;
import io.quarkus.test.QuarkusUnitTest;

class HttpSinkBackpressureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ToUpperCaseSerializer.class, Dto.class, HttpEmitterWithOverflow.class, HttpEndpoint.class))
            .withConfigurationResource("http-sink-backpressure-test-application.properties");

    @Inject
    HttpEmitterWithOverflow emitter;
    @Inject
    HttpEndpoint endpoint;

    @Test
    void shouldThrowExceptionOnOverflow() throws InterruptedException {
        endpoint.pause();

        CompletionStage<Void> emission1 = emitter.emitAndThrowOnOverflow(1);
        assertThrows(IllegalStateException.class, () -> emitter.emitAndThrowOnOverflow(2));

        endpoint.release();
        try {
            emission1.toCompletableFuture().get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            fail("failed waiting for success for the first emission", e);
        }
    }

    @Test
    void shouldBufferPrecisely10() throws InterruptedException {
        endpoint.pause();

        List<CompletionStage<Void>> emissions = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            try {
                emissions.add(emitter.emitAndBufferOnOverflow(i));
            } catch (RuntimeException failure) {
                failures.add(new Failure(i, failure));
            }
        }
        // emitter is set to buffer 10. 1 is being processed, 10 should be buffered and one should fail
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> failures, Matchers.hasSize(1));

        endpoint.release();
        try {
            for (int i = 0; i < 11; i++) {
                emissions.get(0).toCompletableFuture().get(1, TimeUnit.SECONDS);
            }
        } catch (ExecutionException | TimeoutException e) {
            fail("failed waiting for success for the buffered emissions", e);
        }
    }

    @AfterEach
    void cleanUp() {
        endpoint.reset();
    }

    private static class Failure {
        int attempt;
        Throwable failure;

        Failure(int attempt, Throwable failure) {
            this.attempt = attempt;
            this.failure = failure;
        }
    }

}
