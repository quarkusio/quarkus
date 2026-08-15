package io.quarkus.extest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class ExcludedDependencyTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setForcedDependencies(List.of(
                    Dependency.of("org.apache.commons", "commons-text", "1.15.0")))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("org.apache.commons", "commons-text")));

    @Test
    void excludedForcedDependencyIsNotLoadable() {
        assertThat(isClassLoadable("org.apache.commons.text.StringSubstitutor")).isFalse();
    }

    static boolean isClassLoadable(String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
