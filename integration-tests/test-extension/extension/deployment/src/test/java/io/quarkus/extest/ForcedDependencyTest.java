package io.quarkus.extest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class ForcedDependencyTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setForcedDependencies(List.of(
                    Dependency.of("org.apache.commons", "commons-text", "1.15.0")));

    @Test
    void forcedDependencyIsLoadable() {
        boolean loadable;
        try {
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.commons.text.StringSubstitutor");
            loadable = true;
        } catch (ClassNotFoundException e) {
            loadable = false;
        }
        assertThat(loadable).isTrue();
    }
}
