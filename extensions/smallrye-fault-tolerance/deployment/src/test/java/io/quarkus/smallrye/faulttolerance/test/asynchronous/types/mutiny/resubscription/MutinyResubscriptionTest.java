package io.quarkus.smallrye.faulttolerance.test.asynchronous.types.mutiny.resubscription;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MutinyResubscriptionTest {
    // this test verifies resubscription, which is triggered via retry

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MutinyHelloService.class));

    @Inject
    MutinyHelloService service;

    @Test
    public void test() {
        Uni<String> hello = service.hello()
                .onFailure().retry().atMost(2)
                .onFailure().recoverWithItem("hello");

        assertThat(MutinyHelloService.COUNTER).hasValue(0);

        assertThat(hello.await().indefinitely()).isEqualTo("hello");

        // the service.hello() method has @Retry with default settings, so 1 initial attempt + 3 retries = 4 total
        // the onFailure().retry() handler does 1 initial attempt + 2 retries = 3 total
        assertThat(MutinyHelloService.COUNTER).hasValue(4 * 3);
    }
}
