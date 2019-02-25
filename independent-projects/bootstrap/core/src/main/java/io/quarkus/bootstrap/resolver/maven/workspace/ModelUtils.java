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

package io.quarkus.bootstrap.resolver.maven.workspace;

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

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;


/**
 *
 * @author Alexey Loubyansky
 */
public class ModelUtils {

    private static final String STATE_ARTIFACT_INITIAL_VERSION = "1";

    /**
     * Returns the provisioning state artifact for the given application artifact
     *
     * @param appArtifact  application artifact
     * @return  provisioning state artifact
     */
    public static AppArtifact getStateArtifact(AppArtifact appArtifact) {
        return new AppArtifact(appArtifact.getGroupId() + ".quarkus.curate",
                appArtifact.getArtifactId(),
                "",
                "pom",
                STATE_ARTIFACT_INITIAL_VERSION);
    }

    /**
     * Filters out non-platform from application POM dependencies.
     *
     * @param deps  POM model application dependencies
     * @param appDeps  resolved application dependencies
     * @return  dependencies that can be checked for updates
     * @throws AppCreatorException  in case of a failure
     */
    public static List<AppDependency> getUpdateCandidates(List<Dependency> deps, List<AppDependency> appDeps, Set<String> groupIds) throws IOException {
        final Map<AppArtifactKey, AppDependency> appDepMap = new LinkedHashMap<>(appDeps.size());
        for(AppDependency appDep : appDeps) {
            final AppArtifact appArt = appDep.getArtifact();
            appDepMap.put(new AppArtifactKey(appArt.getGroupId(), appArt.getArtifactId(), appArt.getClassifier()), appDep);
        }
        final List<AppDependency> updateCandidates = new ArrayList<>(deps.size());
        // it's critical to preserve the order of the dependencies from the pom
        for(Dependency dep : deps) {
            if(!groupIds.contains(dep.getGroupId()) || "test".equals(dep.getScope())) {
                continue;
            }
            final AppDependency appDep = appDepMap.remove(new AppArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier()));
            if(appDep == null) {
                // This normally would be a dependency that's missing <scope>test</scope> in the artifact's pom
                // but is marked as such in one of artifact's parent poms
                //throw new AppCreatorException("Failed to locate dependency " + new AppArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion()) + " present in pom.xml among resolved application dependencies");
                continue;
            }
            updateCandidates.add(appDep);
        }
        for(AppDependency appDep : appDepMap.values()) {
            if(groupIds.contains(appDep.getArtifact().getGroupId())) {
                updateCandidates.add(appDep);
            }
        }
        return updateCandidates;
    }

    public static AppArtifact resolveAppArtifact(Path appJar) throws IOException {
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
                                    return new AppArtifact(props.getProperty("groupId"), props.getProperty("artifactId"), props.getProperty("version"));
                                }
                            }
                        }
                    }
                }
            }
            throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
        }
    }

    public static Model readAppModel(Path appJar, AppArtifact appArtifact) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, null)) {
            final Path pomXml = fs.getPath("META-INF", "maven", appArtifact.getGroupId(), appArtifact.getArtifactId(), "pom.xml");
            if(!Files.exists(pomXml)) {
                throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.xml in " + appJar);
            }
            return readModel(pomXml);
        }
    }

    static Model readAppModel(Path appJar) throws IOException {
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
                                    final Model model = readModel(pomXml);
                                    Properties props = null;
                                    if(model.getGroupId() == null) {
                                        props = loadPomProps(appJar, artifactIdPath);
                                        final String groupId = props.getProperty("groupId");
                                        if(groupId == null) {
                                            throw new IOException("Failed to determine groupId for " + appJar);
                                        }
                                        model.setGroupId(groupId);
                                    }
                                    if(model.getVersion() == null) {
                                        if(props == null) {
                                            props = loadPomProps(appJar, artifactIdPath);
                                        }
                                        final String version = props.getProperty("version");
                                        if(version == null) {
                                            throw new IOException("Failed to determine the artifact version for " + appJar);
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
            throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.xml in " + appJar);
        }
    }

    private static Properties loadPomProps(Path appJar, Path artifactIdPath) throws IOException {
        final Path propsPath = artifactIdPath.resolve("pom.properties");
        if(!Files.exists(propsPath)) {
            throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
        }
        final Properties props = new Properties();
        try(BufferedReader reader = Files.newBufferedReader(propsPath)) {
            props.load(reader);
        }
        return props;
    }

    public static Model readModel(final Path pomXml) throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(pomXml)) {
            final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            return xpp3Reader.read(reader);
        } catch (XmlPullParserException e) {
            throw new IOException("Failed to parse application POM model", e);
        }
    }

    public static void persistModel(Path pomFile, Model model) throws IOException {
        final MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
        try (BufferedWriter pomFileWriter = Files.newBufferedWriter(pomFile)) {
            xpp3Writer.write(pomFileWriter, model);
        }
    }
}
