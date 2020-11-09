package io.quarkus.maven;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.buildfile.BuildFile;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class MavenProjectBuildFile extends BuildFile {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+)}");

    private Supplier<Model> modelSupplier;
    private Supplier<List<org.eclipse.aether.graph.Dependency>> projectDepsSupplier;
    private Supplier<List<org.eclipse.aether.graph.Dependency>> projectManagedDepsSupplier;
    private Properties projectProps;
    protected List<AppArtifactCoords> dependencies;
    protected List<AppArtifactCoords> managedDependencies;
    protected Model model;

    public MavenProjectBuildFile(Path projectDirPath, QuarkusPlatformDescriptor platformDescriptor, Supplier<Model> model,
            Supplier<List<org.eclipse.aether.graph.Dependency>> projectDeps,
            Supplier<List<org.eclipse.aether.graph.Dependency>> projectManagedDeps,
            Properties projectProps) {
        super(projectDirPath, platformDescriptor);
        this.modelSupplier = model;
        this.projectDepsSupplier = projectDeps;
        this.projectManagedDepsSupplier = projectManagedDeps;
        this.projectProps = projectProps;
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.MAVEN;
    }

    @Override
    protected boolean addDependency(AppArtifactCoords coords, boolean managed) {
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
            DependencyManagement dependencyManagement = model().getDependencyManagement();
            if (dependencyManagement == null) {
                dependencyManagement = new DependencyManagement();
                model().setDependencyManagement(dependencyManagement);
            }
            if (dependencyManagement.getDependencies()
                    .stream()
                    .noneMatch(thisDep -> d.getManagementKey().equals(resolveKey(thisDep)))) {
                dependencyManagement.addDependency(d);
                // the effective managed dependencies set may already include it
                if (!getManagedDependencies().contains(coords)) {
                    getManagedDependencies().add(coords);
                }
                return true;
            }
        } else if (model().getDependencies()
                .stream()
                .noneMatch(thisDep -> d.getManagementKey().equals(thisDep.getManagementKey()))) {
            model().addDependency(d);
            // it could still be a transitive dependency or inherited from the parent
            if (!getDependencies().contains(coords)) {
                getDependencies().add(coords);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void removeDependency(AppArtifactKey key) throws IOException {
        if (model() != null) {
            final Iterator<AppArtifactCoords> i = getDependencies().iterator();
            while (i.hasNext()) {
                final AppArtifactCoords a = i.next();
                if (a.getKey().equals(key)) {
                    i.remove();
                    break;
                }
                model().getDependencies().removeIf(d -> Objects.equals(toKey(d), key));
            }
        }
    }

    @Override
    protected List<AppArtifactCoords> getDependencies() {
        if (dependencies == null) {
            final List<org.eclipse.aether.graph.Dependency> projectDeps = projectDepsSupplier.get();
            projectDepsSupplier = null;
            dependencies = new ArrayList<>(projectDeps.size());
            for (org.eclipse.aether.graph.Dependency dep : projectDeps) {
                org.eclipse.aether.artifact.Artifact a = dep.getArtifact();
                dependencies.add(new AppArtifactCoords(a.getGroupId(), a.getArtifactId(), a.getClassifier(),
                        a.getExtension(), a.getVersion()));
            }
        }
        return dependencies;
    }

    protected List<AppArtifactCoords> getManagedDependencies() {
        if (managedDependencies == null) {
            final List<org.eclipse.aether.graph.Dependency> managedDeps = projectManagedDepsSupplier.get();
            projectManagedDepsSupplier = null;
            managedDependencies = new ArrayList<>(managedDeps.size());
            for (org.eclipse.aether.graph.Dependency dep : managedDeps) {
                org.eclipse.aether.artifact.Artifact a = dep.getArtifact();
                managedDependencies.add(new AppArtifactCoords(a.getGroupId(), a.getArtifactId(), a.getClassifier(),
                        a.getExtension(), a.getVersion()));
            }
        }
        return dependencies;
    }

    @Override
    protected void writeToDisk() throws IOException {
        if (model == null) {
            return;
        }
        try (ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream()) {
            MojoUtils.write(model(), pomOutputStream);
            writeToProjectFile(BuildTool.MAVEN.getDependenciesFile(), pomOutputStream.toByteArray());
        }
    }

    @Override
    protected String getProperty(String propertyName) {
        return projectProps.getProperty(propertyName);
    }

    @Override
    protected void refreshData() {
    }

    private Model model() {
        if (model == null) {
            model = modelSupplier.get().clone();
            modelSupplier = null;
        }
        return model;
    }

    /**
     * Resolves dependencies containing property references in the GAV
     */
    private String resolveKey(Dependency dependency) {
        String resolvedGroupId = toResolvedProperty(dependency.getGroupId());
        String resolvedArtifactId = toResolvedProperty(dependency.getArtifactId());
        String resolvedVersion = toResolvedProperty(dependency.getVersion());
        if (!resolvedGroupId.equals(dependency.getGroupId())
                || !resolvedArtifactId.equals(dependency.getArtifactId())
                || !resolvedVersion.equals(dependency.getVersion())) {
            return resolvedGroupId + ":" + resolvedArtifactId + ":" + dependency.getType()
                    + (dependency.getClassifier() != null ? ":" + dependency.getClassifier() : "");
        }
        return dependency.getManagementKey();
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
