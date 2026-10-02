package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.io.Serializable;

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;

final class ResolvedArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    final ComponentArtifactIdentifier id;
    final File file;
    final String type;

    ResolvedArtifact(ComponentArtifactIdentifier id, File file, String type) {
        this.id = id;
        this.file = file;
        this.type = type;
    }
}
