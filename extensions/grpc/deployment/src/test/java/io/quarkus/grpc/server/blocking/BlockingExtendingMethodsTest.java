package io.quarkus.grpc.server.blocking;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.blocking.BlockingTestServiceGrpc;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.BlockingBaseTestService;
import io.quarkus.grpc.server.services.BlockingExtendingTestService;
import io.quarkus.test.QuarkusUnitTest;

public class BlockingExtendingMethodsTest extends BlockingMethodsBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(EmptyProtos.class.getPackage())
                            .addPackage(Messages.class.getPackage())
                            .addPackage(BlockingTestServiceGrpc.class.getPackage())
                            .addClasses(BlockingBaseTestService.class, BlockingExtendingTestService.class, AssertHelper.class))
            .withConfigurationResource("blocking-test-config.properties");
}
