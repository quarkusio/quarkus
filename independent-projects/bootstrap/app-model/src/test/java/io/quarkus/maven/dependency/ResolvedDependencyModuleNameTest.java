package io.quarkus.maven.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResolvedDependencyModuleNameTest {

    @TempDir
    Path content;

    @Test
    void quteExtensionDoesNotTakeTheQuteEngineModuleName() {
        assertThat(moduleName("io.quarkus", "quarkus-qute")).isEqualTo("io.quarkus.qute.runtime");
    }

    @Test
    void derivedNameDropsTheRepeatedGroupPart() {
        assertThat(moduleName("io.quarkus", "quarkus-cache")).isEqualTo("io.quarkus.cache");
    }

    private String moduleName(String groupId, String artifactId) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion("1.0")
                .setResolvedPath(content)
                .build()
                .getModuleName();
    }
}
