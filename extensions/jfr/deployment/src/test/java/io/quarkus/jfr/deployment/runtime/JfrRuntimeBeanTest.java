package io.quarkus.jfr.deployment.runtime;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.jfr.runtime.internal.runtime.QuarkusRuntimeInfo;
import io.quarkus.runtime.ImageMode;
import io.quarkus.test.QuarkusUnitTest;

public class JfrRuntimeBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    QuarkusRuntimeInfo quarkusRuntimeInfo;

    @Test
    public void test() {
        Assertions.assertEquals(Version.getVersion(), quarkusRuntimeInfo.version());
        Assertions.assertEquals(ImageMode.current().name(), quarkusRuntimeInfo.imageMode());
        Assertions.assertEquals("test", quarkusRuntimeInfo.profiles());
        Assertions.assertEquals(1, quarkusRuntimeInfo.features().stream().filter(s -> s.equals("cdi")).count());
        Assertions.assertEquals(1, quarkusRuntimeInfo.features().stream().filter(s -> s.equals("jfr")).count());
    }
}
