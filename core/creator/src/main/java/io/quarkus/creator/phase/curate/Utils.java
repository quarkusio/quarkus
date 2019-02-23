/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package io.quarkus.creator.phase.curate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class Utils {

    private static final String STATE_ARTIFACT_INITIAL_VERSION = "1";

    /**
     * Returns the provisioning state artifact for the given application artifact
     *
     * @param appArtifact application artifact
     * @return provisioning state artifact
     */
    static AppArtifact getStateArtifact(AppArtifact appArtifact) {
        return new AppArtifact(appArtifact.getGroupId() + ".quarkus.curate",
                appArtifact.getArtifactId(),
                "",
                "pom",
                STATE_ARTIFACT_INITIAL_VERSION);
    }

    /**
     * Filters out non-platform from application POM dependencies.
     *
     * @param deps POM model application dependencies
     * @param appDeps resolved application dependencies
     * @return dependencies that can be checked for updates
     * @throws AppCreatorException in case of a failure
     */
    static List<AppDependency> getUpdateCandidates(List<Dependency> deps, List<AppDependency> appDeps, Set<String> groupIds)
            throws AppCreatorException {
        final Map<ArtifactKey, AppDependency> appDepMap = new LinkedHashMap<>(appDeps.size());
        for (AppDependency appDep : appDeps) {
            appDepMap.put(new ArtifactKey(appDep.getArtifact()), appDep);
        }
        final List<AppDependency> updateCandidates = new ArrayList<>(deps.size());
        // it's critical to preserve the order of the dependencies from the pom
        for (Dependency dep : deps) {
            if (!groupIds.contains(dep.getGroupId()) || "test".equals(dep.getScope())) {
                continue;
            }
            final AppDependency appDep = appDepMap.remove(new ArtifactKey(dep));
            if (appDep == null) {
                // This normally would be a dependency that's missing <scope>test</scope> in the artifact's pom
                // but is marked as such in one of artifact's parent poms
                //throw new AppCreatorException("Failed to locate dependency " + new AppArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion()) + " present in pom.xml among resolved application dependencies");
                continue;
            }
            updateCandidates.add(appDep);
        }
        for (AppDependency appDep : appDepMap.values()) {
            if (groupIds.contains(appDep.getArtifact().getGroupId())) {
                updateCandidates.add(appDep);
            }
        }
        return updateCandidates;
    }

    static AppArtifact resolveAppArtifact(Path appJar) throws AppCreatorException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, null)) {
            final Path metaInfMaven = fs.getPath("META-INF", "maven");
            if (Files.exists(metaInfMaven)) {
                try (DirectoryStream<Path> groupIds = Files.newDirectoryStream(metaInfMaven)) {
                    for (Path groupIdPath : groupIds) {
                        if (!Files.isDirectory(groupIdPath)) {
                            continue;
                        }
                        try (DirectoryStream<Path> artifactIds = Files.newDirectoryStream(groupIdPath)) {
                            for (Path artifactIdPath : artifactIds) {
                                if (!Files.isDirectory(artifactIdPath)) {
                                    continue;
                                }
                                final Path propsPath = artifactIdPath.resolve("pom.properties");
                                if (Files.exists(propsPath)) {
                                    final Properties props = loadPomProps(appJar, artifactIdPath);
                                    return new AppArtifact(props.getProperty("groupId"), props.getProperty("artifactId"),
                                            props.getProperty("version"));
                                }
                            }
                        }
                    }
                }
            }
            throw new AppCreatorException(
                    "Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
        } catch (IOException e) {
            throw new AppCreatorException("Failed to load pom.properties from " + appJar, e);
        }
    }

    static Model readAppModel(Path appJar, AppArtifact appArtifact) throws AppCreatorException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, null)) {
            final Path pomXml = fs.getPath("META-INF", "maven", appArtifact.getGroupId(), appArtifact.getArtifactId(),
                    "pom.xml");
            if (!Files.exists(pomXml)) {
                throw new AppCreatorException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.xml in " + appJar);
            }
            try {
                return readModel(pomXml);
            } catch (IOException e) {
                throw new AppCreatorException("Failed to read " + pomXml, e);
            }
        } catch (IOException e) {
            throw new AppCreatorException("Failed to load pom.xml from " + appJar, e);
        }
    }

    static Model readAppModel(Path appJar) throws AppCreatorException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, null)) {
            final Path metaInfMaven = fs.getPath("META-INF", "maven");
            if (Files.exists(metaInfMaven)) {
                try (DirectoryStream<Path> groupIds = Files.newDirectoryStream(metaInfMaven)) {
                    for (Path groupIdPath : groupIds) {
                        if (!Files.isDirectory(groupIdPath)) {
                            continue;
                        }
                        try (DirectoryStream<Path> artifactIds = Files.newDirectoryStream(groupIdPath)) {
                            for (Path artifactIdPath : artifactIds) {
                                if (!Files.isDirectory(artifactIdPath)) {
                                    continue;
                                }
                                final Path pomXml = artifactIdPath.resolve("pom.xml");
                                if (Files.exists(pomXml)) {
                                    final Model model;
                                    try {
                                        model = readModel(pomXml);
                                    } catch (IOException e) {
                                        throw new AppCreatorException("Failed to read " + pomXml, e);
                                    }
                                    Properties props = null;
                                    if (model.getGroupId() == null) {
                                        props = loadPomProps(appJar, artifactIdPath);
                                        final String groupId = props.getProperty("groupId");
                                        if (groupId == null) {
                                            throw new AppCreatorException("Failed to determine groupId for " + appJar);
                                        }
                                        model.setGroupId(groupId);
                                    }
                                    if (model.getVersion() == null) {
                                        if (props == null) {
                                            props = loadPomProps(appJar, artifactIdPath);
                                        }
                                        final String version = props.getProperty("version");
                                        if (version == null) {
                                            throw new AppCreatorException(
                                                    "Failed to determine the artifact version for " + appJar);
                                        }
                                        model.setVersion(version);
                                    }
                                    return model;
                                }
                            }
                        }
                    }
                }
            }
            throw new AppCreatorException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.xml in " + appJar);
        } catch (IOException e) {
            throw new AppCreatorException("Failed to load pom.xml from " + appJar, e);
        }
    }

    private static Properties loadPomProps(Path appJar, Path artifactIdPath) throws AppCreatorException {
        final Path propsPath = artifactIdPath.resolve("pom.properties");
        if (!Files.exists(propsPath)) {
            throw new AppCreatorException(
                    "Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
        }
        final Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(propsPath)) {
            props.load(reader);
        } catch (IOException e) {
            throw new AppCreatorException("Failed to read " + propsPath, e);
        }
        return props;
    }

    static Model readModel(final Path pomXml) throws IOException, AppCreatorException {
        try (BufferedReader reader = Files.newBufferedReader(pomXml)) {
            final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            return xpp3Reader.read(reader);
        } catch (XmlPullParserException e) {
            throw new AppCreatorException("Failed to parse application POM model", e);
        }
    }

    public static void persistModel(Path pomFile, Model model) throws AppCreatorException {
        final MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
        try (BufferedWriter pomFileWriter = Files.newBufferedWriter(pomFile)) {
            xpp3Writer.write(pomFileWriter, model);
        } catch (IOException e) {
            throw new AppCreatorException("Faile to write the pom.xml file", e);
        }
    }

    private static class ArtifactKey {
        final String groupId;
        final String artifactId;
        final String classifier;

        ArtifactKey(AppArtifact artifact) {
            this.groupId = artifact.getGroupId();
            this.artifactId = artifact.getArtifactId();
            this.classifier = artifact.getClassifier();
        }

        ArtifactKey(Dependency artifact) {
            this.groupId = artifact.getGroupId();
            this.artifactId = artifact.getArtifactId();
            final String classifier = artifact.getClassifier();
            this.classifier = classifier == null ? "" : classifier;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ArtifactKey other = (ArtifactKey) obj;
            if (artifactId == null) {
                if (other.artifactId != null)
                    return false;
            } else if (!artifactId.equals(other.artifactId))
                return false;
            if (classifier == null) {
                if (other.classifier != null)
                    return false;
            } else if (!classifier.equals(other.classifier))
                return false;
            if (groupId == null) {
                if (other.groupId != null)
                    return false;
            } else if (!groupId.equals(other.groupId))
                return false;
            return true;
        }
    }

}
