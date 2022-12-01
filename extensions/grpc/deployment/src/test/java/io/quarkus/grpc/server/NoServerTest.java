package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that no server are started / produced if there is no services
 */
public class NoServerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    public void test() {
        assertThat(GrpcServerRecorder.getVerticleCount()).isZero();
    }
}
