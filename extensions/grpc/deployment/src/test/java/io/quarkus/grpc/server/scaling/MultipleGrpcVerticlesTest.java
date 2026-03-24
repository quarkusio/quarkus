package io.quarkus.grpc.server.scaling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.quarkus.test.QuarkusExtensionTest;

public class MultipleGrpcVerticlesTest extends ScalingTestBase {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClass(ThreadReturningGreeterService.class))
            .withConfigurationResource("multiple-instances-config.properties");

    @Test
    public void shouldUseMultipleThreads() throws InterruptedException, TimeoutException, ExecutionException {
        Set<String> threads = getThreadsUsedFor100Requests();

        assertThat(threads).hasSizeGreaterThan(1);
    }

}
