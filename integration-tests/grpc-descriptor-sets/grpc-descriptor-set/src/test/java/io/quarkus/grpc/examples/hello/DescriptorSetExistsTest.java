package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class DescriptorSetExistsTest {

    @Test
    public void descriptorSetExists() {
        var expectedOutputDir = Path.of(System.getProperty("build.dir"))
                .resolve("generated-sources")
                .resolve("grpc");

        assertThat(expectedOutputDir).exists();
        assertThat(expectedOutputDir.resolve("descriptor_set.dsc"))
                .exists()
                .isNotEmptyFile();
    }
}
