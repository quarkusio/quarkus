/*
 *
 *   Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version
 *   2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */
package org.jboss.shamrock.maven.components.dependencies;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.jboss.shamrock.maven.MavenConstants.PLUGIN_GROUPID;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Component(role = Extensions.class, instantiationStrategy = "singleton")
public class Extensions {

    /**
     * Maven Project Builder component.
     */
    @Requirement
    protected ProjectBuilder projectBuilder;

    /**
     * Component used to resolve artifacts and download their files from remote repositories.
     */
    @Requirement
    protected ArtifactResolver artifactResolver;


    public List<Extension> get() {
        ObjectMapper mapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        URL url = Extensions.class.getClassLoader().getResource("extensions.json");
        try {
            return mapper.readValue(url, new TypeReference<List<Extension>>() {
                // Do nothing.
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the extensions.json file", e);
        }
    }

    public Dependency parse(String dependency, Log log) {
        Dependency res = new Dependency();
        String[] segments = dependency.split(":");
        if (segments.length >= 2) {
            res.setGroupId(segments[0]);
            res.setArtifactId(segments[1]);
            if (segments.length >= 3 && !segments[2].isEmpty()) {
                res.setVersion(segments[2]);
            }
            if (segments.length >= 4) {
                res.setClassifier(segments[3]);
            }
            return res;
        } else {
            log.warn("Invalid dependency description '" + dependency + "'");
            return null;
        }
    }

    private List<Dependency> getDependenciesFromBom(MavenSession session, List<ArtifactRepository> repositories) throws MojoExecutionException {
        String bomCoordinates = PLUGIN_GROUPID + ":" + MojoUtils.get("bom-artifactId") + ":"
                + MojoUtils.get("shamrock-version");
        MavenProject bom = getMavenProject(bomCoordinates, session, repositories);
        return bom.getDependencyManagement().getDependencies();
    }

    public boolean addExtensions(Model model, List<String> extensions,
                                 MavenSession session, List<ArtifactRepository> repositories, Log log) throws MojoExecutionException {
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }

        boolean updated = false;
        List<Extension> exts = get();
        List<Dependency> dependenciesFromBom = getDependenciesFromBom(session, repositories);
        for (String dependency : extensions) {
            Optional<Extension> optional = exts.stream()
                    .filter(d -> {
                        boolean hasTag = d.labels().contains(dependency.trim().toLowerCase());
                        boolean machName = d.getName().toLowerCase().contains(dependency.trim().toLowerCase());
                        return hasTag || machName;
                    })
                    .findAny();

            if (optional.isPresent()) {
                if (!MojoUtils.hasDependency(model, optional.get().getGroupId(), optional.get().getArtifactId())) {
                    log.info("Adding extension " + optional.get().toCoordinates());

                    if (containsBOM(model) && isDefinedInBom(dependenciesFromBom, optional.get())) {
                        model.addDependency(optional.get().toDependency(true));
                    } else {
                        model.addDependency(optional.get().toDependency(false));
                    }

                    updated = true;
                } else {
                    log.info("Extension already present - skipping");
                }

            } else if (dependency.contains(":")) {
                // Add it as a dependency
                // groupId:artifactId:version:classifier
                Dependency parsed = parse(dependency, log);
                if (parsed != null) {
                    log.info("Adding dependency " + parsed.getManagementKey());
                    model.addDependency(parsed);
                    updated = true;
                }
            } else {
                log.warn("Cannot find a dependency matching '" + dependency + "'");
            }
        }

        return updated;
    }

    private boolean isDefinedInBom(List<Dependency> dependencies, Extension extension) {
        return dependencies.stream().anyMatch(dependency ->
                dependency.getGroupId().equalsIgnoreCase(extension.getGroupId())
                        && dependency.getArtifactId().equalsIgnoreCase(extension.getArtifactId()));
    }

    private boolean containsBOM(Model model) {
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equalsIgnoreCase(dependency.getScope()))
                .filter(dependency -> "pom".equalsIgnoreCase(dependency.getType()))
                // Does it matches the bom artifact name
                .anyMatch(dependency -> dependency.getArtifactId().equalsIgnoreCase(MojoUtils.get("bom-artifactId")));
    }

    /**
     * Retrieves the Maven Project associated with the given artifact String, in the form of
     * <code>groupId:artifactId[:version]</code>. This resolves the POM artifact at those coordinates and then builds
     * the Maven project from it.
     *
     * @param artifactString Coordinates of the Maven project to get.
     * @param session        the maven session
     * @param repositories   the repositories
     * @return New Maven project.
     * @throws MojoExecutionException If there was an error while getting the Maven project.
     */
    protected MavenProject getMavenProject(String artifactString, MavenSession session, List<ArtifactRepository> repositories)
            throws MojoExecutionException {
        ArtifactCoordinate coordinate = getArtifactCoordinate(artifactString, "pom");
        try {
            ProjectBuildingRequest pbr = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            pbr.setRemoteRepositories(repositories);
            pbr.setProject(null);
            pbr.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            pbr.setResolveDependencies(true);
            Artifact artifact = artifactResolver.resolveArtifact(pbr, coordinate).getArtifact();
            return projectBuilder.build(artifact.getFile(), pbr).getProject();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to get the POM for the artifact '" + artifactString
                    + "'. Verify the artifact parameter.", e);
        }
    }

    /**
     * Parses the given String into GAV artifact coordinate information, adding the given type.
     *
     * @param artifactString should respect the format <code>groupId:artifactId[:version]</code>
     * @param type           The extension for the artifact, must not be <code>null</code>.
     * @return the <code>Artifact</code> object for the <code>artifactString</code> parameter.
     * @throws MojoExecutionException if the <code>artifactString</code> doesn't respect the format.
     */
    protected ArtifactCoordinate getArtifactCoordinate(String artifactString, String type)
            throws MojoExecutionException {
        String groupId; // required
        String artifactId; // required
        String version; // optional

        String[] artifactParts = artifactString.split(":");
        switch (artifactParts.length) {
            case 2:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = Artifact.LATEST_VERSION;
                break;
            case 3:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = artifactParts[2];
                break;
            default:
                throw new MojoExecutionException("The artifact parameter '" + artifactString
                        + "' should be conform to: " + "'groupId:artifactId[:version]'.");
        }
        return getArtifactCoordinate(groupId, artifactId, version, type);
    }

    protected ArtifactCoordinate getArtifactCoordinate(String groupId, String artifactId, String version, String type) {
        DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
        coordinate.setGroupId(groupId);
        coordinate.setArtifactId(artifactId);
        coordinate.setVersion(version);
        coordinate.setExtension(type);
        return coordinate;
    }

}
