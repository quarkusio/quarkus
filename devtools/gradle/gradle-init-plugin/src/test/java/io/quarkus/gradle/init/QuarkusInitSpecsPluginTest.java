package io.quarkus.gradle.init;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.buildinit.specs.BuildInitSpec;
import org.gradle.buildinit.specs.internal.BuildInitSpecRegistry;
import org.junit.jupiter.api.Test;

class QuarkusInitSpecsPluginTest {

    @Test
    void registersQuarkusAppSpecOnApply() {
        BuildInitSpecRegistry registry = new BuildInitSpecRegistry();
        QuarkusInitSpecsPlugin plugin = new QuarkusInitSpecsPlugin(registry);

        plugin.apply(null);

        BuildInitSpec registered = registry.getSpecByType("quarkus-app");
        assertThat(registered).isNotNull();
        assertThat(registry.getGeneratorForSpec(registered)).isEqualTo(QuarkusAppInitGenerator.class);
    }
}
