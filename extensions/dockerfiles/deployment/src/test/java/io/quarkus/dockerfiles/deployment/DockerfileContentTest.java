package io.quarkus.dockerfiles.deployment;

import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DockerfileContentTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void shouldRenderDockerfileJvm() {
        Assertions.assertNotNull(
                DockerfileContent.getJvmDockerfileContent("registry.access.redhat.com/ubi8/openjdk-21:1.19", "my-app",
                        Paths.get("target")));
    }
}
