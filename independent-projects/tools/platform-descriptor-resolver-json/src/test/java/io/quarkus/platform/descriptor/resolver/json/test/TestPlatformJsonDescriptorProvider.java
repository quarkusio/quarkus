package io.quarkus.platform.descriptor.resolver.json.test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.platform.tools.ToolsConstants;

public class TestPlatformJsonDescriptorProvider implements TsArtifact.ContentProvider {

    private final TsArtifact bomArtifact;


    public TestPlatformJsonDescriptorProvider(TsArtifact bomArtifact) {
        this.bomArtifact = bomArtifact;
    }


    @Override
    public Path getPath(Path workDir) throws IOException {
        final Model model = bomArtifact.getPomModel();

        final JsonObject json = Json.object();

        json.set("bom", Json.object().set("groupId", model.getGroupId()).set("artifactId", model.getArtifactId()).set("version", model.getVersion()));

        String coreVersion = null;
        for(Dependency dep : model.getDependencyManagement().getDependencies()) {
            if(dep.getArtifactId().equals(ToolsConstants.QUARKUS_CORE_ARTIFACT_ID)
                    && dep.getGroupId().equals(ToolsConstants.QUARKUS_CORE_GROUP_ID)) {
                coreVersion = dep.getVersion();
            }
        }
        if(coreVersion == null) {
            throw new IllegalStateException("Failed to locate " + ToolsConstants.QUARKUS_CORE_GROUP_ID + ":" + ToolsConstants.QUARKUS_CORE_ARTIFACT_ID + " among the managed dependencies");
        }
        json.set("quarkus-core-version", coreVersion);

        final Path jsonPath = workDir.resolve(bomArtifact.getArtifactFileName());
        try(BufferedWriter writer = Files.newBufferedWriter(jsonPath)) {
            json.writeTo(writer);
        }
        return jsonPath;
    }
}
