package io.quarkus.smallrye.faulttolerance.test.asynchronous.types.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MutinyFaultToleranceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MutinyHelloService.class));

    @Inject
    MutinyHelloService service;

    @BeforeEach
    public void setUp() {
        MutinyHelloService.COUNTER.set(0);
    }

    @Test
    public void nonblocking() {
        Uni<String> hello = service.helloNonblocking();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }

    @Test
    public void blocking() {
        Uni<String> hello = service.helloBlocking();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronous() {
        Uni<String> hello = service.helloAsynchronous();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonBlocking() {
        Uni<String> hello = service.helloAsynchronousNonBlocking();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonblockingCombined() {
        Uni<String> hello = service.helloAsynchronousNonblockingCombined();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousBlockingCombined() {
        Uni<String> hello = service.helloAsynchronousBlockingCombined();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }
}
