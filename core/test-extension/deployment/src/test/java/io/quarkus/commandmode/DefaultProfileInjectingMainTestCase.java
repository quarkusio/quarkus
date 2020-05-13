package io.quarkus.commandmode;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class DefaultProfileInjectingMainTestCase {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PropertyInjectingMain.class))
            .setApplicationName("property-injecting")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("test.message", "hello from ${quarkus.profile}")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("hello from prod").contains("prod activated");
        Assertions.assertThat(config.getExitCode()).isEqualTo(0);
    }

}
