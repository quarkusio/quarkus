package io.quarkus.smallrye.faulttolerance.test.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.faulttolerance.test.fallback.FallbackBean.RecoverFallback;
import io.quarkus.test.QuarkusUnitTest;

public class FallbackTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FallbackBean.class));

    @Inject
    FallbackBean bean;

    @Test
    public void testFallback() {
        assertEquals(RecoverFallback.class.getName(), bean.ping());
    }

}
