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
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.jboss.logging.Logger;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalProject {

    private static final Logger log = Logger.getLogger(LocalProject.class);

    private static final String POM_XML = "pom.xml";

    public static LocalProject resolveLocalProject(Path currentProjectDir) throws BootstrapException {
        try {
            return new LocalProject(currentProjectDir, null);
        } catch (IOException e) {
            throw new BootstrapException("Failed to resolve local Maven project for " + currentProjectDir, e);
        }
    }

    public static LocalProject resolveLocalProjectWithWorkspace(Path currentProjectDir) throws BootstrapException {

        final Path rootDir = locateRootProjectDir(currentProjectDir);
        log.debugf("Root project dir %s", rootDir);

        final LocalWorkspace workspace = new LocalWorkspace();
        final LocalProject project;
        try {
            project = loadProject(workspace, rootDir, currentProjectDir);
        } catch (IOException e) {
            throw new BootstrapException("Failed to resolve local Maven projects for " + currentProjectDir, e);
        }
        if(project == null) {
            throw new BootstrapException("Failed to locate current project among the loaded local projects");
        }
        return project;
    }

    private static LocalProject loadProject(LocalWorkspace workspace, Path dir, Path currentProjectDir) throws IOException {
        final LocalProject project = new LocalProject(dir, workspace);
        final Path projectDir = project.getDir();
        LocalProject result = currentProjectDir == null || !currentProjectDir.equals(projectDir) ? null : project;
        final List<String> modules = project.getRawModel().getModules();
        if (!modules.isEmpty()) {
            Path dirArg = result == null ? currentProjectDir : null;
            for (String module : modules) {
                final LocalProject loaded = loadProject(workspace, projectDir.resolve(module), dirArg);
                if(loaded != null && result == null) {
                    result = loaded;
                    dirArg = null;
                }
            }
        }
        return result;
    }

    private static Path locateRootProjectDir(Path currentProjectDir) throws BootstrapException {
        Path p = currentProjectDir;
        while(true) {
            final Path parentDir = p.getParent();
            if(parentDir == null) {
                return p;
            }
            if(!Files.exists(parentDir.resolve(POM_XML))) {
                return p;
            }
            p = parentDir;
        }
    }

    public static Path locateCurrentProjectDir(Path path) throws BootstrapException {
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
    private final LocalWorkspace workspace;

    private LocalProject(Path dir, LocalWorkspace workspace) throws IOException {
        this.dir = dir;
        this.workspace = workspace;
        final Path pomXml = dir.resolve(POM_XML);
        rawModel = ModelUtils.readModel(pomXml);
        rawModel.setPomFile(pomXml.toFile());
        final Parent parent = rawModel.getParent();
        String groupId = rawModel.getGroupId();
        if(groupId == null) {
            if(parent == null) {
                throw new IOException("Failed to determine groupId for " + pomXml);
            }
            this.groupId = parent.getGroupId();
        } else {
            this.groupId = groupId;
        }

        this.artifactId = rawModel.getArtifactId();
        String version = rawModel.getVersion();
        if(version == null) {
            if(parent == null) {
                throw new IOException("Failed to determine version for " + pomXml);
            }
            this.version = parent.getVersion();
        } else {
            this.version = version;
        }
        if(workspace != null) {
            workspace.addProject(this, pomXml.toFile().lastModified());
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public Path getDir() {
        return dir;
    }

    public Path getOutputDir() {
        return dir.resolve("target");
    }

    public Path getClassesDir() {
        return getOutputDir().resolve("classes");
    }

    public Model getRawModel() {
        return rawModel;
    }

    public LocalWorkspace getWorkspace() {
        return workspace;
    }

    public AppArtifactKey getKey() {
        return new AppArtifactKey(groupId, artifactId);
    }

    public AppArtifact getAppArtifact() {
        final AppArtifact appArtifact = new AppArtifact(groupId, artifactId, "", rawModel.getPackaging(), version);
        appArtifact.setPath(getClassesDir());
        return appArtifact;
    }
}
