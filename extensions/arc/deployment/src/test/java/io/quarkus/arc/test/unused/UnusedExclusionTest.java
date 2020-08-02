package io.quarkus.arc.test.unused;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.unused.subpackage.Beta;
import io.quarkus.test.QuarkusUnitTest;

public class UnusedExclusionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(UnusedExclusionTest.class, Alpha.class, Beta.class, Charlie.class, Delta.class,
                            ProducerBean.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.unremovable-types=io.quarkus.arc.test.unused.UnusedExclusionTest$Alpha,io.quarkus.arc.test.unused.subpackage.**,io.quarkus.arc.test.unused.Charlie,Delta"),
                            "application.properties"));

    @Test
    public void testBeansWereNotRemoved() {
        ArcContainer container = Arc.container();
        String expectedBeanResponse = "ok";
        InstanceHandle<Alpha> alphaInstance = container.instance(Alpha.class);
        Assertions.assertTrue(alphaInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, alphaInstance.get().ping());

        InstanceHandle<Beta> betaInstance = container.instance(Beta.class);
        Assertions.assertTrue(betaInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, betaInstance.get().ping());

        InstanceHandle<Charlie> charlieInstance = container.instance(Charlie.class);
        Assertions.assertTrue(charlieInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, charlieInstance.get().ping());

        InstanceHandle<Delta> deltaInstance = container.instance(Delta.class);
        Assertions.assertTrue(deltaInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, deltaInstance.get().ping());
    }

    // unused bean, won't be removed
    @ApplicationScoped
    static class Alpha {

        public String ping() {
            return "ok";
        }

    }
}
