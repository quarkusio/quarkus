/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppArtifactResolverBase;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class ResolvedGradleArtifactDeps extends AppArtifactResolverBase {

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String type;
    private final String version;
    private final List<AppDependency> deps;
    private List<File> dependencyFiles;
    private File projectFile;

    public ResolvedGradleArtifactDeps(Project project) {
        groupId = project.getGroup().toString();
        artifactId = project.getName();
        //not sure what we should set here
        classifier = "";
        //TODO: set this to jar for now
        type = "jar";
        version = project.getVersion().toString();

        deps = new ArrayList<>(extractDependencies(project));
    }

    private Set<AppDependency> extractDependencies(Project project) {
        Set<AppDependency> dependencies = new HashSet<>();

        Configuration configuration = project.getConfigurations().getByName("compileOnly");
        ResolutionResult resolutionResult = configuration.getIncoming().getResolutionResult();
        ResolvedComponentResult root = resolutionResult.getRoot();

        //TODO: Ideally the dependencyFiles should be added to the AppArtifacts while traversing the dependencies
        // - atm this is the only way to fetch the dependency files
        dependencyFiles = new ArrayList<>();
        SourceSet sourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
        sourceSet.getCompileClasspath().forEach(dependencyFiles::add);

        //dirty hack to add the project target
        projectFile = new File(project.getBuildDir().getAbsolutePath() + File.separator +
                "libs" + File.separator +
                artifactId + "-" + version + ".jar");

        traverseDependencies(root.getDependencies(), dependencies);
        return dependencies;
    }

    private void traverseDependencies(Set<? extends DependencyResult> dependencies, Set<AppDependency> appDependencies) {
        dependencies.forEach(dependency -> {
            if (dependency instanceof ResolvedDependencyResult) {
                ResolvedComponentResult result = ((ResolvedDependencyResult) dependency).getSelected();
                appDependencies.add(new AppDependency(toAppArtifact(result), "provided"));
                traverseDependencies(result.getDependencies(), appDependencies);
            }
        });
    }

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doResolve(AppArtifact coords) throws AppCreatorException {
        if (coords.getGroupId().equals(groupId) && coords.getArtifactId().equals(artifactId)) {
            setPath(coords, projectFile.toPath());
        } else {

            File dep = findMatchingDependencyFile(coords.getGroupId(), coords.getArtifactId(), coords.getVersion());
            if (dep != null)
                setPath(coords, dep.toPath());
            else
                throw new AppCreatorException("Did not find dependency file for: " + coords.toString());
        }
    }

    private AppArtifact toAppArtifact(ResolvedComponentResult result) {
        AppArtifact appArtifacat = new AppArtifact(result.getModuleVersion().getGroup(),
                result.getModuleVersion().getName(), result.getModuleVersion().getVersion());

        File dep = findMatchingDependencyFile(appArtifacat.getGroupId(), appArtifacat.getArtifactId(),
                appArtifacat.getVersion());
        if (dep != null) {
            setPath(appArtifacat, dep.toPath());
        }
        return appArtifacat;
    }

    private File findMatchingDependencyFile(String group, String artifact, String version) {
        String searchCriteria1 = group + File.separator + artifact + "-" + version;
        String searchCriteria2 = group + File.separator + artifact + File.separatorChar + version;
        String searchCriteria3 = group.replace('.', File.separatorChar) +
                File.separatorChar + artifact + File.separatorChar + version;
        for (File file : dependencyFiles) {
            if (file.getPath().contains(searchCriteria1) ||
                    file.getPath().contains(searchCriteria2) ||
                    file.getPath().contains(searchCriteria3))
                return file;
        }
        return null;
    }

    @Override
    public List<AppDependency> collectDependencies(AppArtifact coords) throws AppCreatorException {
        if (!coords.getGroupId().equals(groupId) ||
                !coords.getArtifactId().equals(artifactId) ||
                !coords.getClassifier().equals(classifier) ||
                !coords.getType().equals(type) ||
                !coords.getVersion().equals(version)) {
            throw new AppCreatorException("The resolve can only resolve dependencies for " + groupId + ':' + artifactId + ':'
                    + classifier + ':' + type + ':' + version + ": " + coords);
        }
        return deps;
    }

    @Override
    public List<AppDependency> collectDependencies(AppArtifact root, List<AppDependency> deps) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listLaterVersions(AppArtifact artifact, String upToVersion, boolean inclusive)
            throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNextVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLatestVersion(AppArtifact artifact, String upToVersion, boolean inclusive) throws AppCreatorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "ResolvedGradleArtifactDeps{\n" +
                "groupId='" + groupId + '\n' +
                "artifactId='" + artifactId + '\n' +
                "classifier='" + classifier + '\n' +
                "type='" + type + '\n' +
                "version='" + version + '\n' +
                "deps=" + deps + '\n' +
                "dependencyFiles=" + dependencyFiles + '\n' +
                "projectFile=" + projectFile + '\n' +
                '}';
    }
}
