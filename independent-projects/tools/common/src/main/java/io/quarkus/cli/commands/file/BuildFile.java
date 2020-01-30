package io.quarkus.cli.commands.file;

import static io.quarkus.maven.utilities.MojoUtils.credentials;
import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.Printer;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public abstract class BuildFile implements Closeable {

    protected final static Printer PRINTER = new Printer();

    private final ProjectWriter writer;

    private final BuildTool buildTool;

    public BuildFile(final ProjectWriter writer, BuildTool buildTool) {
        this.writer = writer;
        this.buildTool = buildTool;
    }

    protected void write(String fileName, String content) throws IOException {
        writer.write(fileName, content);
    }

    public boolean addDependency(QuarkusPlatformDescriptor platform, Extension extension) throws IOException {
        if (!hasDependency(extension)) {
            PRINTER.ok(" Adding extension " + extension.managementKey());
            Dependency dep;
            if(containsBOM(platform.getBomGroupId(), platform.getBomArtifactId()) && isDefinedInBom(platform.getManagedDependencies(), extension)) {
                dep = extension.toDependency(true);
            } else {
                dep = extension.toDependency(false);
                if(getProperty(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_VERSION_NAME) != null) {
                    dep.setVersion(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_VERSION_VALUE);
                }
            }
            addDependencyInBuildFile(dep);
            return true;
        } else {
            PRINTER.noop(" Skipping already present extension " + extension.managementKey());
            return false;
        }
    }

    protected abstract void addDependencyInBuildFile(Dependency dependency) throws IOException;

    protected abstract boolean hasDependency(Extension extension) throws IOException;

    public boolean addExtensionAsGAV(String query) throws IOException {
        Dependency parsed = MojoUtils.parse(query.trim());
        boolean alreadyThere = getDependencies().stream()
                .anyMatch(d -> d.getManagementKey().equalsIgnoreCase(parsed.getManagementKey()));
        if (!alreadyThere) {
            PRINTER.ok(" Adding dependency " + parsed.getManagementKey());
            addDependencyInBuildFile(parsed);
            return true;
        } else {
            PRINTER.noop(" Skipping already present dependency " + parsed.getManagementKey());
            return false;
        }
    }

    protected boolean isDefinedInBom(List<Dependency> dependencies, Extension extension) {
        return dependencies.stream().anyMatch(dependency -> dependency.getGroupId().equalsIgnoreCase(extension.getGroupId())
                && dependency.getArtifactId().equalsIgnoreCase(extension.getArtifactId()));
    }

    protected abstract boolean containsBOM(String groupId, String artifactId) throws IOException;

    public abstract List<Dependency> getDependencies() throws IOException;

    public Map<String, Dependency> findInstalled() throws IOException {
        return mapDependencies(getDependencies(), loadManaged());
    }

    private Map<String, Dependency> loadManaged() throws IOException {
        final List<Dependency> managedDependencies = getManagedDependencies();
        return managedDependencies.isEmpty() ? Collections.emptyMap()
                : mapDependencies(managedDependencies, Collections.emptyMap());
    }

    protected Map<String, Dependency> mapDependencies(final List<Dependency> dependencies,
            final Map<String, Dependency> managed) {
        final Map<String, Dependency> map = new TreeMap<>();

        if (dependencies != null) {
            final List<Dependency> listed = dependencies.stream()
                    // THIS ASSUMES EXTENSIONS' groupId is always 'io.quarkus' which is wrong
                    .filter(new QuarkusDependencyPredicate())
                    .collect(toList());

            listed.forEach(d -> {
                if (d.getVersion() == null) {
                    final Dependency managedDep = managed.get(credentials(d));
                    if (managedDep != null) {
                        final String version = managedDep.getVersion();
                        if (version != null) {
                            d.setVersion(version);
                        }
                    }
                }

                map.put(credentials(d), d);
            });
        }
        return map;
    }

    public abstract String getProperty(String propertyName) throws IOException;

    protected abstract List<Dependency> getManagedDependencies() throws IOException;

    public abstract void completeFile(String groupId, String artifactId, String version, QuarkusPlatformDescriptor platform, Properties props) throws IOException;

    public BuildTool getBuildTool() {
        return buildTool;
    }

    protected ProjectWriter getWriter() {
        return writer;
    }

}
