/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.quarkus.gradle.model.config;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

public final class LocalComponentOutputViews {

    private final ArtifactView classes;
    private final ArtifactView resources;
    private final ArtifactView jars;

    private LocalComponentOutputViews(ObjectFactory objects, Configuration configuration) {
        classes = artifactView(objects, configuration, LibraryElements.CLASSES,
                ArtifactTypeDefinition.JVM_CLASS_DIRECTORY);
        resources = artifactView(objects, configuration, LibraryElements.RESOURCES,
                ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY);
        jars = artifactView(objects, configuration, LibraryElements.JAR,
                ArtifactTypeDefinition.JAR_TYPE);
    }

    public static LocalComponentOutputViews of(ObjectFactory objects, Configuration configuration) {
        return new LocalComponentOutputViews(objects, configuration);
    }

    public ArtifactView classes() {
        return classes;
    }

    public ArtifactView resources() {
        return resources;
    }

    public ArtifactView jars() {
        return jars;
    }

    public Provider<Set<ResolvedArtifactResult>> classArtifacts() {
        return classes.getArtifacts().getResolvedArtifacts();
    }

    public Provider<Set<ResolvedArtifactResult>> resourceArtifacts() {
        return resources.getArtifacts().getResolvedArtifacts();
    }

    public Provider<Set<ResolvedArtifactResult>> jarArtifacts() {
        return jars.getArtifacts().getResolvedArtifacts();
    }

    public FileCollection classFiles() {
        return classes.getFiles();
    }

    public FileCollection resourceFiles() {
        return resources.getFiles();
    }

    public FileCollection jarFiles() {
        return jars.getFiles();
    }

    public Provider<Set<File>> jarFilesWithoutOutputVariants(ProviderFactory providers) {
        return providers.provider(() -> {
            Set<String> componentsWithOutputVariants = new HashSet<>();
            collectComponentIds(classArtifacts().get(), componentsWithOutputVariants);
            collectComponentIds(resourceArtifacts().get(), componentsWithOutputVariants);
            Set<File> jarFiles = new LinkedHashSet<>();
            for (ResolvedArtifactResult artifact : jarArtifacts().get()) {
                if (!componentsWithOutputVariants.contains(componentId(artifact))) {
                    jarFiles.add(artifact.getFile());
                }
            }
            return jarFiles;
        });
    }

    private static ArtifactView artifactView(ObjectFactory objects, Configuration configuration,
            String libraryElements, String artifactType) {
        return configuration.getIncoming().artifactView(view -> {
            view.withVariantReselection();
            view.lenient(true);
            view.attributes(attributes -> {
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements.class, libraryElements));
                attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, artifactType);
            });
        });
    }

    private static void collectComponentIds(Set<ResolvedArtifactResult> artifacts, Set<String> target) {
        for (ResolvedArtifactResult artifact : artifacts) {
            target.add(componentId(artifact));
        }
    }

    private static String componentId(ResolvedArtifactResult artifact) {
        return artifact.getId().getComponentIdentifier().getDisplayName();
    }
}
