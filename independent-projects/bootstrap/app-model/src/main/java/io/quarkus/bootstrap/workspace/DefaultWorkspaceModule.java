package io.quarkus.bootstrap.workspace;

import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class DefaultWorkspaceModule implements WorkspaceModule, Serializable {

    private final WorkspaceModuleId id;
    private final File moduleDir;
    private final File buildDir;
    private final Collection<ProcessedSources> mainSources = new ArrayList<>(1);
    private final Collection<ProcessedSources> mainResources = new ArrayList<>(1);
    private final Collection<ProcessedSources> testSources = new ArrayList<>(1);
    private final Collection<ProcessedSources> testResources = new ArrayList<>(1);
    private PathCollection buildFiles;

    public DefaultWorkspaceModule(WorkspaceModuleId id, File moduleDir, File buildDir) {
        super();
        this.id = id;
        this.moduleDir = moduleDir;
        this.buildDir = buildDir;
    }

    @Override
    public WorkspaceModuleId getId() {
        return id;
    }

    @Override
    public File getModuleDir() {
        return moduleDir;
    }

    @Override
    public File getBuildDir() {
        return buildDir;
    }

    @Override
    public Collection<ProcessedSources> getMainSources() {
        return mainSources;
    }

    public void addMainSources(ProcessedSources mainSources) {
        this.mainSources.add(mainSources);
    }

    @Override
    public Collection<ProcessedSources> getMainResources() {
        return mainResources;
    }

    public void addMainResources(ProcessedSources mainResources) {
        this.mainResources.add(mainResources);
    }

    @Override
    public Collection<ProcessedSources> getTestSources() {
        return testSources;
    }

    public void addTestSources(ProcessedSources testSources) {
        this.testSources.add(testSources);
    }

    @Override
    public Collection<ProcessedSources> getTestResources() {
        return testResources;
    }

    public void addTestResources(ProcessedSources testResources) {
        this.testResources.add(testResources);
    }

    public void setBuildFiles(PathCollection buildFiles) {
        this.buildFiles = buildFiles;
    }

    @Override
    public PathCollection getBuildFiles() {
        return buildFiles == null ? PathList.empty() : buildFiles;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(id);
        buf.append(" ").append(moduleDir);
        appendSources(buf, "sources", getMainSources());
        appendSources(buf, "resources", getMainResources());
        appendSources(buf, "test-sources", getTestSources());
        appendSources(buf, "test-resources", getTestResources());
        return buf.toString();
    }

    private void appendSources(StringBuilder buf, String name, Collection<ProcessedSources> sources) {
        if (!sources.isEmpty()) {
            buf.append(" ").append(name).append("[");
            final Iterator<ProcessedSources> i = sources.iterator();
            buf.append(i.next());
            while (i.hasNext()) {
                buf.append(";").append(i.next());
            }
            buf.append("]");
        }
    }
}
