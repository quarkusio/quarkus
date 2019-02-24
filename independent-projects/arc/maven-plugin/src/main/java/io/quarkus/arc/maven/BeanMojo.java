/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.ResourceOutput;

/**
 *
 * @author Martin Kouba
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BeanMojo extends AbstractMojo {

    private static final String CLASS_FILE_EXTENSION = ".class";

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}/generated-sources")
    private File targetDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter
    private String[] additionalBeanDefiningAnnotations;

    @Parameter
    private String[] dependenciesToScan;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Index index;
        try {
            index = createIndex();
        } catch (IOException e) {
            throw new MojoFailureException("Failed to create index", e);
        }

        BeanProcessor beanProcessor = BeanProcessor.builder().setIndex(index).setAdditionalBeanDefiningAnnotations(getAdditionalBeanDefiningAnnotations().stream().map((s) -> new BeanDefiningAnnotation(s, null)).collect(Collectors.toSet()))
                .setOutput(new ResourceOutput() {

                    @Override
                    public void writeResource(Resource resource) throws IOException {
                        switch (resource.getType()) {
                            case JAVA_CLASS:
                                resource.writeTo(outputDirectory);
                                break;
                            case SERVICE_PROVIDER:
                                resource.writeTo(new File(outputDirectory, "/META-INF/services/"));
                            default:
                                break;
                        }
                    }
                }).build();
        try {
            beanProcessor.process();
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating resources", e);
        }
    }

    private Index createIndex() throws IOException {
        Indexer indexer = new Indexer();

        // Index dependencies
        if (dependenciesToScan != null && dependenciesToScan.length > 0) {
            List<File> dependenciesToIndex = project.getArtifacts().stream().filter(this::isDependencyToScan).map(Artifact::getFile)
                    .collect(Collectors.toList());
            for (File dependency : dependenciesToIndex) {
                index(indexer, dependency, this::isClassJarEntry);
            }
        }

        // Index output directory, i.e. target/classes
        Files.walkFileTree(outputDirectory.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    try (InputStream stream = Files.newInputStream(file)) {
                        indexer.index(stream);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return indexer.complete();
    }

    private void index(Indexer indexer, File jarFile, Predicate<JarEntry> predicate) throws IOException {
        JarFile jar = new JarFile(jarFile);
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (predicate.test(entry)) {
                    try (InputStream stream = jar.getInputStream(entry)) {
                        indexer.index(stream);
                    }
                }
            }
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }

    private Collection<DotName> getAdditionalBeanDefiningAnnotations() {
        List<DotName> beanDefiningAnnotations = Collections.emptyList();
        if (additionalBeanDefiningAnnotations != null && additionalBeanDefiningAnnotations.length > 0) {
            beanDefiningAnnotations = new ArrayList<>();
            for (String beanDefiningAnnotation : additionalBeanDefiningAnnotations) {
                beanDefiningAnnotations.add(DotName.createSimple(beanDefiningAnnotation));
            }
        }
        return beanDefiningAnnotations;
    }

    private boolean isDependencyToScan(Artifact artifact) {
        String id = artifact.getGroupId() + ":" + artifact.getArtifactId();
        for (String dependency : dependenciesToScan) {
            if (dependency.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClassJarEntry(JarEntry entry) {
        return entry.getName().endsWith(CLASS_FILE_EXTENSION);
    }

}