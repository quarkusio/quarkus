package io.quarkus.core.deployment.action.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.action.ActionBuilder;

class PhaseServicesTest {

    enum TestEnum {
        FOO,
        BAR
    }

    @Test
    void testAfterEnumConstant() {
        List<ServiceMetadataBuildItem> metadataItems = new ArrayList<>();
        ActionBuilder builder = createBuilder(metadataItems);

        builder.forService(String.class)
                .after(TestEnum.FOO)
                .action(ctx -> "test");

        assertThat(metadataItems).hasSize(1);
        ServiceMetadataBuildItem meta = metadataItems.get(0);
        assertThat(meta.dependencies()).hasSize(1);
        Dependency dep = meta.dependencies().get(0);
        assertThat(dep.key()).isEqualTo("io.quarkus.core.deployment.action.impl.PhaseServicesTest$TestEnum:FOO");
    }

    @Test
    void testAfterPhaseEnumConstant() {
        List<ServiceMetadataBuildItem> metadataItems = new ArrayList<>();
        ActionBuilder builder = createBuilder(metadataItems);

        builder.forService(String.class)
                .after(Phase.DATA)
                .action(ctx -> "test");

        assertThat(metadataItems).hasSize(1);
        ServiceMetadataBuildItem meta = metadataItems.get(0);
        assertThat(meta.dependencies()).hasSize(1);
        Dependency dep = meta.dependencies().get(0);
        assertThat(dep.key()).isEqualTo("io.quarkus.deployment.Phase:DATA");
    }

    @Test
    void testBeforeEnumConstant() {
        List<ServiceMetadataBuildItem> metadataItems = new ArrayList<>();
        ActionBuilder builder = createBuilder(metadataItems);

        builder.forService(String.class)
                .before(TestEnum.BAR)
                .action(ctx -> "test");

        // The metadata item won't have beforeKeys, but we can verify it doesn't throw.
        assertThat(metadataItems).hasSize(1);
    }

    @Test
    void testBeforePhaseEnumConstantProhibited() {
        List<ServiceMetadataBuildItem> metadataItems = new ArrayList<>();
        ActionBuilder builder = createBuilder(metadataItems);

        assertThatThrownBy(() -> {
            builder.forService(String.class)
                    .before(Phase.DATA)
                    .action(ctx -> "test");
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot declare a 'before' dependency on Phase");
    }

    @Test
    void testBeforePhaseClassProhibited() {
        List<ServiceMetadataBuildItem> metadataItems = new ArrayList<>();
        ActionBuilder builder = createBuilder(metadataItems);

        assertThatThrownBy(() -> {
            builder.forService(String.class)
                    .before(Phase.class)
                    .action(ctx -> "test");
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot declare a 'before' dependency on Phase");
    }

    private static ActionBuilder createBuilder(List<ServiceMetadataBuildItem> metadataItems) {
        return new ActionBuilderImpl(
                m -> {
                },
                s -> {
                },
                metadataItems::add,
                s -> {
                },
                r -> {
                },
                "testBuildStep");
    }
}
