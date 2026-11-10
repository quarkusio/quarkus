package io.quarkus.it.resteasy.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.ClassInclusionReport;

@QuarkusIntegrationTest
public class NativeHttp3AbsencePruningIT extends GreetingResourceTest {

    @Test
    void testNativeBuildLogs() throws IOException {
        Path executable = Path.of(System.getProperty("native.image.path"));
        if (!Files.exists(executable)) {
            executable = executable.resolveSibling(executable.getFileName() + ".exe");
        }
        assertThat(executable).isRegularFile();

        Path stdout = executable.resolveSibling(executable.getFileName() + "-build-stdout.log");
        Path stderr = executable.resolveSibling(executable.getFileName() + "-build-stderr.log");
        assertThat(stdout).isRegularFile();
        assertThat(Files.readString(stdout)).contains("GraalVM Native Image", "Finished generating");
        // A successful build may produce no stderr, or compiler-specific warnings.
        assertThat(stderr).isRegularFile();
    }

    @Test
    public void verifyPruning() {
        ClassInclusionReport report = ClassInclusionReport.load();
        report.assertContainsNot("io.netty.handler.codec.quic.Quic");
        report.assertContainsNot("io.netty.handler.codec.quic.Quiche");
    }
}
