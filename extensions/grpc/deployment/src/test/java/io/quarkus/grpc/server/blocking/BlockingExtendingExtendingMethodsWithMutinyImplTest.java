package io.quarkus.grpc.server.blocking;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.blocking.BlockingTestServiceGrpc;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.BlockingMutinyBaseExtendingTestService;
import io.quarkus.grpc.server.services.BlockingMutinyBaseTestService;
import io.quarkus.grpc.server.services.BlockingMutinyExtendingExtendingTestService;
import io.quarkus.test.QuarkusUnitTest;

public class BlockingExtendingExtendingMethodsWithMutinyImplTest extends BlockingMethodsWithMutinyBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(EmptyProtos.class.getPackage())
                            .addPackage(Messages.class.getPackage())
                            .addPackage(BlockingTestServiceGrpc.class.getPackage())
                            .addClasses(BlockingMutinyBaseTestService.class, BlockingMutinyBaseExtendingTestService.class,
                                    BlockingMutinyExtendingExtendingTestService.class, AssertHelper.class))
            .withConfigurationResource("blocking-test-config.properties");
}
