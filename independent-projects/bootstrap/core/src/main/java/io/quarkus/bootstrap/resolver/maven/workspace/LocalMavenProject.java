/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Resource;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.ArtifactResolver;
import io.quarkus.bootstrap.resolver.LocalProject;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalMavenProject implements LocalProject {

    public static final String PROJECT_GROUPID = "${project.groupId}";

    private static final String PROJECT_BASEDIR = "${project.basedir}";
    private static final String POM_XML = "pom.xml";

    public static LocalMavenProject load(Path path) throws BootstrapException {
        return new LocalMavenProject(readModel(locateCurrentProjectDir(path).resolve(POM_XML)), null);
    }

    public static LocalMavenProject loadWorkspace(Path path) throws BootstrapException {
        final Path currentProjectDir = locateCurrentProjectDir(path);
        final LocalMavenWorkspace ws = new LocalMavenWorkspace();
        final LocalMavenProject project = load(ws, null, loadRootModel(currentProjectDir), currentProjectDir);
        return project == null ? load(ws, null, readModel(currentProjectDir.resolve(POM_XML)), currentProjectDir) : project;
    }

    private static LocalMavenProject load(LocalMavenWorkspace workspace, LocalMavenProject parent, Model model, Path currentProjectDir) throws BootstrapException {
        final LocalMavenProject project = new LocalMavenProject(model, workspace);
        if(parent != null) {
            parent.modules.add(project);
        }
        LocalMavenProject result = currentProjectDir == null || !currentProjectDir.equals(project.getDir()) ? null : project;
        final List<String> modules = project.getRawModel().getModules();
        if (!modules.isEmpty()) {
            Path dirArg = result == null ? currentProjectDir : null;
            for (String module : modules) {
                final LocalMavenProject loaded = load(workspace, project, readModel(project.getDir().resolve(module).resolve(POM_XML)), dirArg);
                if(loaded != null && result == null) {
                    result = loaded;
                    dirArg = null;
                }
            }
        }
        return result;
    }

    private static Model loadRootModel(Path currentProjectDir) throws BootstrapException {
        Path pomXml = currentProjectDir.resolve(POM_XML);
        Model model = readModel(pomXml);
        Parent parent = model.getParent();
        while(parent != null) {
            if(parent.getRelativePath() != null) {
                pomXml = pomXml.getParent().resolve(parent.getRelativePath()).normalize();
                if(!Files.exists(pomXml)) {
                    return model;
                }
                if(Files.isDirectory(pomXml)) {
                    pomXml = pomXml.resolve(POM_XML);
                }
            } else {
                pomXml = pomXml.getParent().getParent().resolve(POM_XML);
                if(!Files.exists(pomXml)) {
                    return model;
                }
            }
            model = readModel(pomXml);
            parent = model.getParent();
        }
        return model;
    }

    private static final Model readModel(Path pom) throws BootstrapException {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (IOException e) {
            throw new BootstrapException("Failed to read " + pom, e);
        }
    }

    private static Path locateCurrentProjectDir(Path path) throws BootstrapException {
        Path p = path;
        while(p != null) {
            if(Files.exists(p.resolve(POM_XML))) {
                return p;
            }
            p = p.getParent();
        }
        throw new BootstrapException("Failed to locate project pom.xml for " + path);
    }

    private final Model rawModel;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Path dir;
    private final LocalMavenWorkspace workspace;
    private final List<LocalMavenProject> modules = new ArrayList<>(0);

    private LocalMavenProject(Model rawModel, LocalMavenWorkspace workspace) throws BootstrapException {
        this.rawModel = rawModel;
        this.dir = rawModel.getProjectDirectory().toPath();
        this.workspace = workspace;
        final Parent parent = rawModel.getParent();
        String groupId = rawModel.getGroupId();
        if(groupId == null) {
            if(parent == null) {
                throw new BootstrapException("Failed to determine groupId for " + rawModel.getPomFile());
            }
            this.groupId = parent.getGroupId();
        } else {
            this.groupId = groupId;
        }

        this.artifactId = rawModel.getArtifactId();
        String version = rawModel.getVersion();
        if(version == null) {
            if(parent == null) {
                throw new BootstrapException("Failed to determine version for " + rawModel.getPomFile());
            }
            this.version = parent.getVersion();
        } else {
            this.version = version;
        }
        if(workspace != null) {
            workspace.addProject(this, rawModel.getPomFile().lastModified());
        }
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public Path getDir() {
        return dir;
    }

    @Override
    public Path getOutputDir() {
        return dir.resolve("target");
    }

    public Path getClassesDir() {
        return getOutputDir().resolve("classes");
    }

    public Path getSourcesSourcesDir() {
        if (getRawModel().getBuild() != null && getRawModel().getBuild().getSourceDirectory() != null) {
            String originalValue = getRawModel().getBuild().getSourceDirectory();
            return Paths.get(originalValue.startsWith(PROJECT_BASEDIR) ? originalValue.replace(PROJECT_BASEDIR, this.dir.toString()) : originalValue);
        }
        return dir.resolve("src/main/java");
    }

    public Path getResourcesSourcesDir() {
        if(getRawModel().getBuild() != null && getRawModel().getBuild().getResources() != null) {
            for (Resource i : getRawModel().getBuild().getResources()) {
                //todo: support multiple resources dirs for config hot deployment
                return Paths.get(i.getDirectory());
            }
        }
        return dir.resolve("src/main/resources");
    }

    public Model getRawModel() {
        return rawModel;
    }

    @Override
    public LocalMavenWorkspace getWorkspace() {
        return workspace;
    }

    public AppArtifactKey getKey() {
        return new AppArtifactKey(groupId, artifactId);
    }

    @Override
    public AppArtifact getAppArtifact() {
        final AppArtifact appArtifact = new AppArtifact(groupId, artifactId, BootstrapConstants.EMPTY, rawModel.getPackaging(), version);
        appArtifact.setPath(getClassesDir());
        return appArtifact;
    }

    public List<LocalMavenProject> getSelfWithLocalDeps() {
        if(workspace == null) {
            return Collections.singletonList(this);
        }
        final List<LocalMavenProject> ordered = new ArrayList<>();
        collectSelfWithLocalDeps(this, new HashSet<>(),  ordered);
        return ordered;
    }

    private static void collectSelfWithLocalDeps(LocalMavenProject project, Set<AppArtifactKey> addedDeps, List<LocalMavenProject> ordered) {
        if(!project.modules.isEmpty()) {
            for(LocalMavenProject module : project.modules) {
                collectSelfWithLocalDeps(module, addedDeps, ordered);
            }
        }
        for(Dependency dep : project.getRawModel().getDependencies()) {
            final AppArtifactKey depKey = project.getKey(dep);
            final LocalMavenProject localDep = project.workspace.getProject(depKey);
            if(localDep == null || addedDeps.contains(depKey)) {
                continue;
            }
            collectSelfWithLocalDeps(localDep, addedDeps, ordered);
        }
        if(addedDeps.add(project.getKey())) {
            ordered.add(project);
        }
    }

    private AppArtifactKey getKey(Dependency dep) {
        return new AppArtifactKey(PROJECT_GROUPID.equals(dep.getGroupId()) ? getGroupId() : dep.getGroupId(), dep.getArtifactId());
    }

    @Override
    public ArtifactResolver getArtifactResolver() {
        return offline -> MavenArtifactResolver.getDeploymentDependencies(this, offline);
    }

    @Override
    public void close() {
        // Does nothing for Maven
    }
}
