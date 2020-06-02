package io.quarkus.devtools.project.buildfile;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public class MavenBuildFile extends BuildFile {

    private AtomicReference<Model> modelRef = new AtomicReference<>();

    public MavenBuildFile(final Path projectFolderPath, final QuarkusPlatformDescriptor platformDescriptor) {
        super(projectFolderPath, platformDescriptor);
    }

    @Override
    public void writeToDisk() throws IOException {
        if (getModel() == null) {
            return;
        }
        try (ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream()) {
            MojoUtils.write(getModel(), pomOutputStream);
            writeToProjectFile(BuildTool.MAVEN.getDependenciesFile(), pomOutputStream.toByteArray());
        }
    }

    @Override
    protected void addDependencyInBuildFile(AppArtifactCoords coords) throws IOException {
        if (getModel() != null) {
            final Dependency d = new Dependency();
            d.setGroupId(coords.getGroupId());
            d.setArtifactId(coords.getArtifactId());
            d.setVersion(coords.getVersion());
            // When classifier is empty, you get  <classifier></classifier> in the pom.xml
            if (coords.getClassifier() != null && !coords.getClassifier().isEmpty()) {
                d.setClassifier(coords.getClassifier());
            }
            d.setType(coords.getType());
            getModel().addDependency(d);
        }
    }

    @Override
    protected void removeDependencyFromBuildFile(AppArtifactKey key) throws IOException {
        if (getModel() != null) {
            getModel().getDependencies()
                    .removeIf(d -> Objects.equals(toKey(d), key));
        }
    }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        return getModel() == null ? Collections.emptyList() : getModel().getDependencies();
    }

    @Override
    protected boolean containsBOM(String groupId, String artifactId) throws IOException {
        if (getModel() == null || getModel().getDependencyManagement() == null) {
            return false;
        }
        List<Dependency> dependencies = getModel().getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equals(dependency.getScope()))
                .filter(dependency -> "pom".equals(dependency.getType()))
                // Does it matches the bom artifact name
                .anyMatch(dependency -> dependency.getArtifactId()
                        .equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE)
                        && dependency.getGroupId().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_GROUP_ID_VALUE));
    }

    @Override
    protected void refreshData() {
        this.modelRef.set(null);
    }

    @Override
    public String getProperty(String propertyName) throws IOException {
        if (getModel() == null) {
            return null;
        }
        return getModel().getProperties().getProperty(propertyName);
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.MAVEN;
    }

    private Model getModel() throws IOException {
        return modelRef.updateAndGet(model -> {
            if (model == null) {
                try {
                    return initModel();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return model;
        });
    }

    private Model initModel() throws IOException {
        byte[] content = readProjectFile(BuildTool.MAVEN.getDependenciesFile());
        return MojoUtils.readPom(new ByteArrayInputStream(content));
    }
}
