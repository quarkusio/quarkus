package io.quarkus.hibernate.accessor.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorRegistrationBuildItem.Builder;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorStrategy;

class HibernateAccessorTransformationTest {
    @Test
    void fieldReadersRequireTheirDeclaringClassAsHost() throws IOException {
        var index = Index.of(Model.class, Subclass.class);
        var inheritedField = index.getClassByName(Model.class).field("value");
        var builder = new Builder(index.getClassByName(Subclass.class)).source("test registration");
        assertThatThrownBy(() -> builder.addFieldReader(inheritedField))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Model.class.getName() + "#value")
                .hasMessageContaining(Subclass.class.getName())
                .hasMessageContaining("test registration");
    }

    @Test
    void fieldWritersRequireTheirDeclaringClassAsHost() throws IOException {
        var index = Index.of(Model.class, Subclass.class);
        var inheritedField = index.getClassByName(Model.class).field("value");
        var builder = new Builder(index.getClassByName(Subclass.class)).source("test registration");
        assertThatThrownBy(() -> builder.addFieldWriter(inheritedField))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Model.class.getName() + "#value")
                .hasMessageContaining(Subclass.class.getName())
                .hasMessageContaining("test registration");
    }

    @Test
    void reflectionOnlyProducesNeitherTransformersNorClasses() throws IOException {
        var type = Index.of(Model.class).getClassByName(Model.class);
        var registration = new Builder(type).addFieldWriter(type.field("value")).build();
        List<BytecodeTransformerBuildItem> transformers = new ArrayList<>();
        List<GeneratedClassBuildItem> classes = new ArrayList<>();
        new HibernateAccessorProcessor().generateDirectAccessors(() -> HibernateAccessorStrategy.REFLECTION,
                List.of(registration), classes::add, transformers::add);
        assertThat(transformers).isEmpty();
        assertThat(classes).isEmpty();
    }

    static class Model {
        final String value = new String("original");
    }

    /** Declares no fields of its own, so {@code value} can only be registered against its declaring class. */
    static class Subclass extends Model {
    }
}
