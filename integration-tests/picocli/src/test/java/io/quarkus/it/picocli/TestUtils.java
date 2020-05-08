package io.quarkus.it.picocli;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.test.QuarkusProdModeTest;

class TestUtils {

    private TestUtils() {
    }

    static QuarkusProdModeTest createConfig(String appName, Class<?>... classes) {
        return new QuarkusProdModeTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(classes))
                .setApplicationName(appName).setApplicationVersion("0.1-SNAPSHOT")
                .setExpectExit(true).setRun(true);
    }

}
