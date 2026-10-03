package io.quarkus.gradle.model.tasks;

import java.io.File;
import java.io.Serializable;

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;

class QuarkusResolvedArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    final ComponentArtifactIdentifier id;
    final String type;
    final File file;

    QuarkusResolvedArtifact(ComponentArtifactIdentifier id, File file, String type) {
        this.id = id;
        this.type = type;
        this.file = file;
    }
}
