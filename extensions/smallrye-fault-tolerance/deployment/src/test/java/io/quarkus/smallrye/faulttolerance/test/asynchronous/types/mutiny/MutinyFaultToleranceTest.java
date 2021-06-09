package io.quarkus.smallrye.faulttolerance.test.asynchronous.types.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MutinyFaultToleranceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MutinyHelloService.class));

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
    public void asynchronousNonblocking() {
        Uni<String> hello = service.helloAsynchronousNonblocking();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousBlocking() {
        Uni<String> hello = service.helloAsynchronousBlocking();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(MutinyHelloService.COUNTER).hasValue(4);
    }
}
