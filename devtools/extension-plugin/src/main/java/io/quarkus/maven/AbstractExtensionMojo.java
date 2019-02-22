/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.quarkus.maven;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.*;

import io.quarkus.dependencies.Extension;

/**
 * @author <a href="http://kenfinnigan.me">Ken Finnigan</a>
 */
public abstract class AbstractExtensionMojo extends AbstractMojo {

    @Inject
    protected MavenSession mavenSession;

    @Inject
    public ProjectBuilder projectBuilder;

    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    private static List<MavenProject> PROBABLE_EXTENSIONS = null;

    protected synchronized Set<Extension> extensions() throws MojoExecutionException {
        return probableExtensionProjects()
                .stream()
                .map(ExtensionRegistry.INSTANCE::of)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<MavenProject> probableExtensionProjects() throws MojoExecutionException {
        if (null == PROBABLE_EXTENSIONS) {
            PROBABLE_EXTENSIONS = mavenSession.getAllProjects()
                    .stream()
                    .filter(this::isExtension)
                    .collect(Collectors.toList());

            if (PROBABLE_EXTENSIONS.size() < 5) {
                getLog().warn("MavenSession does not contain all extensions, rebuilding the project hierarchy directly");
                buildProjects();
            }
        }

        return PROBABLE_EXTENSIONS;
    }

    private void buildProjects() throws MojoExecutionException {
        final ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        request.setProcessPlugins(false);
        request.setSystemProperties(System.getProperties());
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        request.setRepositorySession(mavenSession.getRepositorySession());
        request.setResolveDependencies(false);

        try {
            PROBABLE_EXTENSIONS = this.projectBuilder
                    .build(Collections.singletonList(findRoot(this.project).getFile()), true, request)
                    .stream()
                    .map(ProjectBuildingResult::getProject)
                    .collect(Collectors.toList());
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Error generating list of PROBABLE_EXTENSIONS", e);
        }
    }

    private MavenProject findRoot(MavenProject current) {
        if (current.getArtifactId().equals("quarkus-parent")) {
            return current;
        }
        return findRoot(current.getParent());
    }

    private boolean isExtension(MavenProject project) {
        //TODO Change how we determine what is the "extension" artifact when Toronto model implemented
        return project.getArtifactId().contains("-deployment");
    }
}
