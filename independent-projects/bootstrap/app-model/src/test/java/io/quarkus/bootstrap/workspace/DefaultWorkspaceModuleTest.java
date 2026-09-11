package io.quarkus.bootstrap.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultWorkspaceModuleTest {

    @Test
    void roundTripsAnIdentityOnlyWorkspaceModule() {
        WorkspaceModule original = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "library", "1.0"))
                .build();

        WorkspaceModule restored = WorkspaceModule.builder().fromMap(original.asMap()).build();

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getModuleDir()).isNull();
        assertThat(restored.getBuildDir()).isNull();
        assertThat(restored.getBuildFiles()).isEmpty();
    }
}
