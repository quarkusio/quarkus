package io.quarkus.deployment.pkg.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

class UberJarIgnoredResourceBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClass(UberJarMain.class))
            .setApplicationName("uber-jar-ignored")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true)
            .overrideConfigKey("quarkus.package.type", "uber-jar")
            .setForcedDependencies(
                    Collections.singletonList(
                            // META-INF/cxf/cxf.fixml should be present in the cxf-rt-transports-http and cxf-core JARs
                            new AppArtifact("org.apache.cxf", "cxf-rt-transports-http", "3.4.3")));

    @Test
    public void testResourceWasIgnored() throws IOException {
        assertThat(runner.getStartupConsoleOutput()).contains("RESOURCES: 0");
        assertThat(runner.getExitCode()).isZero();
    }

    @QuarkusMain
    public static class UberJarMain {

        public static void main(String[] args) throws IOException {
            List<URL> resources = Collections
                    .list(UberJarMain.class.getClassLoader().getResources("META-INF/cxf/cxf.fixml"));
            System.out.println("RESOURCES: " + resources.size());
        }

    }
}
