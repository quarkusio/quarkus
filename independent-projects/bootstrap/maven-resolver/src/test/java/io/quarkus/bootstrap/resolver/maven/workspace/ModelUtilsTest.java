package io.quarkus.bootstrap.resolver.maven.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ModelUtilsTest {

    @Test
    void resolveVersion_literal() {
        assertThat(ModelUtils.resolveVersion("1.0.0", new Model())).isEqualTo("1.0.0");
    }

    @ParameterizedTest
    @ValueSource(strings = { "${revision}", "${sha1}", "${changelist}" })
    void resolveVersion_notResolvable(String rawVersion) {
        assertThat(ModelUtils.resolveVersion(rawVersion, new Model())).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "revision", "sha1", "changelist" })
    void resolveVersion_resolvable(String ciFriendlyPropertyName) {
        var model = new Model();
        model.getProperties().put(ciFriendlyPropertyName, "1.0.0");

        assertThat(ModelUtils.resolveVersion("${" + ciFriendlyPropertyName + "}", model)).isEqualTo("1.0.0");
    }

    @Test
    void resolveVersion_allCiFriendlyPropertyNames() {
        var model = new Model();
        model.getProperties().put("revision", "1");
        model.getProperties().put("sha1", "2");
        model.getProperties().put("changelist", "3");

        assertThat(ModelUtils.resolveVersion("${revision}.${sha1}.${changelist}", model)).isEqualTo("1.2.3");
    }

    @Test
    // better error message than "named capturing group is missing trailing '}'"
    void resolveVersion_illegalPlaceholder_missingTrailing() {
        var model = new Model();
        model.getProperties().put("revision", "${main.project.version}");
        model.getProperties().put("main.project.version", "1.6.0-SNAPSHOT");

        assertThatThrownBy(() -> ModelUtils.resolveVersion("${revision}", model))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("revision", "${main.project.version}");
    }

    @Test
    // better error message than "No group with name {version}"
    // see also https://github.com/quarkusio/quarkus/issues/22171
    void resolveVersion_illegalPlaceholder_noGroup() {
        var model = new Model();
        model.getProperties().put("revision", "${version}.${build}");
        model.getProperties().put("version", "1.0.0");
        model.getProperties().put("build", "0-SNAPSHOT");

        assertThatThrownBy(() -> ModelUtils.resolveVersion("${revision}", model))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("revision", "${version}");
    }
}
