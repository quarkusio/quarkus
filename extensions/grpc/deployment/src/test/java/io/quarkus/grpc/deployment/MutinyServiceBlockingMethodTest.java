package io.quarkus.grpc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.server.services.BlockingMutinyHelloService;

/**
 * Verify that methods annotated with @Blocking from services implementing the MutinyService interface are considered
 * blocking.
 */
public class MutinyServiceBlockingMethodTest {

    @Test
    public void testBlocking() throws Exception {
        Class<?> clazz = BlockingMutinyHelloService.class;
        DotName className = DotName.createSimple(clazz.getName());

        Indexer indexer = new Indexer();
        indexer.indexClass(BlockingMutinyHelloService.class);
        Index index = indexer.complete();

        ClassInfo classInfo = index.getClassByName(className);

        assertThat(GrpcServerProcessor.gatherBlockingOrVirtualMethodNames(classInfo, index, false))
                .containsExactlyInAnyOrderElementsOf(List.of("sayHello", "wEIRD"));
    }

}
