package io.quarkus.cli.commands.file;

import static io.quarkus.maven.utilities.MojoUtils.credentials;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.Printer;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;

public abstract class BuildFile {

    protected final static Printer PRINTER = new Printer();

    private final ProjectWriter writer;

    public BuildFile(final ProjectWriter writer) {
        this.writer = writer;
    }

    protected void write(String fileName, String content) throws IOException {
        writer.write(fileName, content);
    }

    public abstract void write() throws IOException;

    public boolean addDependency(List<Dependency> dependenciesFromBom, Extension extension) {
        if (!hasDependency(extension)) {
            PRINTER.ok(" Adding extension " + extension.managementKey());
            addDependencyInBuildFile(extension
                    .toDependency(containsBOM() &&
                            isDefinedInBom(dependenciesFromBom, extension)));
            return true;
        } else {
            PRINTER.noop(" Skipping already present extension " + extension.managementKey());
            return false;
        }
    }

    protected abstract void addDependencyInBuildFile(Dependency dependency);

    protected abstract boolean hasDependency(Extension extension);

    public boolean addExtensionAsGAV(String query) {
        Dependency parsed = MojoUtils.parse(query.trim().toLowerCase());
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

    protected abstract boolean containsBOM();

    public abstract List<Dependency> getDependencies();

    public Map<String, Dependency> findInstalled() {
        return mapDependencies(getDependencies(), loadManaged());
    }

    private Map<String, Dependency> loadManaged() {
        final List<Dependency> managedDependencies = getManagedDependencies();
        return managedDependencies.isEmpty() ? Collections.emptyMap()
                : mapDependencies(managedDependencies, Collections.emptyMap());
    }

    protected Map<String, Dependency> mapDependencies(final List<Dependency> dependencies,
            final Map<String, Dependency> managed) {
        final Map<String, Dependency> map = new TreeMap<>();

        if (dependencies != null) {
            final List<Dependency> listed = dependencies.stream()
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

    public abstract String getProperty(String propertyName);

    protected abstract List<Dependency> getManagedDependencies();

}
