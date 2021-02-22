package io.quarkus.devtools.project.buildfile;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.extensions.Extensions;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

public class MavenBuildFile extends BuildFile {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+)}");

    private final AtomicReference<Model> modelRef = new AtomicReference<>();

    public MavenBuildFile(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor) {
        super(projectDirPath, platformDescriptor);
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
    protected boolean addDependency(AppArtifactCoords coords, boolean managed) {
        Model model = getModel();
        final Dependency d = new Dependency();
        d.setGroupId(coords.getGroupId());
        d.setArtifactId(coords.getArtifactId());
        if (!managed) {
            d.setVersion(coords.getVersion());
        }
        // When classifier is empty, you get  <classifier></classifier> in the pom.xml
        if (coords.getClassifier() != null && !coords.getClassifier().isEmpty()) {
            d.setClassifier(coords.getClassifier());
        }
        d.setType(coords.getType());
        if ("pom".equalsIgnoreCase(coords.getType())) {
            d.setScope("import");
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            if (dependencyManagement == null) {
                dependencyManagement = new DependencyManagement();
                model.setDependencyManagement(dependencyManagement);
            }
            if (dependencyManagement.getDependencies()
                    .stream()
                    .map(this::toResolvedDependency)
                    .noneMatch(thisDep -> d.getManagementKey().equals(thisDep.getManagementKey()))) {
                dependencyManagement.addDependency(d);
                return true;
            }
        } else {
            if (model.getDependencies()
                    .stream()
                    .noneMatch(thisDep -> d.getManagementKey().equals(thisDep.getManagementKey()))) {
                model.addDependency(d);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void removeDependency(AppArtifactKey key) throws IOException {
        if (getModel() != null) {
            getModel().getDependencies()
                    .removeIf(d -> Objects.equals(toKey(d), key));
        }
    }

    @Override
    public List<AppArtifactCoords> getDependencies() throws IOException {
        return getModel() == null ? Collections.emptyList()
                : getModel().getDependencies().stream().map(Extensions::toCoords).collect(Collectors.toList());
    }

    @Override
    protected void refreshData() {
        this.modelRef.set(null);
    }

    @Override
    public String getProperty(String propertyName) {
        if (getModel() == null) {
            return null;
        }
        return getModel().getProperties().getProperty(propertyName);
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.MAVEN;
    }

    private Model getModel() {
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
        if (!hasProjectFile(BuildTool.MAVEN.getDependenciesFile())) {
            return null;
        }
        byte[] content = readProjectFile(BuildTool.MAVEN.getDependenciesFile());
        return MojoUtils.readPom(new ByteArrayInputStream(content));
    }

    /**
     * Resolves dependencies containing property references in the GAV
     */
    private Dependency toResolvedDependency(Dependency dependency) {
        String resolvedGroupId = toResolvedProperty(dependency.getGroupId());
        String resolvedArtifactId = toResolvedProperty(dependency.getArtifactId());
        String resolvedVersion = toResolvedProperty(dependency.getVersion());
        if (!resolvedGroupId.equals(dependency.getGroupId())
                || !resolvedArtifactId.equals(dependency.getArtifactId())
                || !resolvedVersion.equals(dependency.getVersion())) {
            Dependency newDep = dependency.clone();
            newDep.setGroupId(resolvedGroupId);
            newDep.setArtifactId(resolvedArtifactId);
            newDep.setVersion(resolvedVersion);
            return newDep;
        }
        return dependency;
    }

    /**
     * Resolves properties as ${quarkus.platform.version}
     */
    private String toResolvedProperty(String value) {
        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        if (matcher.matches()) {
            String property = getProperty(matcher.group(1));
            return property == null ? value : property;
        }
        return value;
    }
}
