package io.quarkus.grpc.server.blocking;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.dam.Blocking2;
import com.dam.MutinyBlocking2Grpc;

import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class BlockingConfusingOnClassTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(Blocking2.class.getPackage())
                            .addClasses(BlockingConfusingOnClassTest.Blocking2Service.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @GrpcService
    @NonBlocking
    @Blocking
    public static class Blocking2Service extends MutinyBlocking2Grpc.Blocking2ImplBase {
        @Override
        public Uni<com.dam.Blocking.ThreadName> returnThread1(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    com.dam.Blocking.ThreadName.newBuilder().setName(message).build());
        }

        @Override
        public Uni<com.dam.Blocking.ThreadName> returnThread2(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    com.dam.Blocking.ThreadName.newBuilder().setName(message).build());
        }
    }
}