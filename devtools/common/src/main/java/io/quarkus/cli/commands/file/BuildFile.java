package io.quarkus.cli.commands.file;

import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.Printer;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

public abstract class BuildFile {

    protected final static Printer PRINTER = new Printer();

    private final ProjectWriter writer;

    public BuildFile(final ProjectWriter writer) {
        this.writer = writer;
    }

    protected void write(String fileName, String content) throws IOException {
        getWriter().write(fileName, content);
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
            PRINTER.noop(" Skipping extension " + extension.managementKey() + ": already present");
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
            PRINTER.noop(" Dependency " + parsed.getManagementKey() + " already in the pom.xml file - skipping");
            return false;
        }
    }

    protected boolean isDefinedInBom(List<Dependency> dependencies, Extension extension) {
        return dependencies.stream().anyMatch(dependency -> dependency.getGroupId().equalsIgnoreCase(extension.getGroupId())
                && dependency.getArtifactId().equalsIgnoreCase(extension.getArtifactId()));
    }

    public ProjectWriter getWriter() {
        return writer;
    }

    protected abstract boolean containsBOM();

    protected abstract List<Dependency> getDependencies();

}
