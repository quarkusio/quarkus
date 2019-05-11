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

package io.quarkus.bootstrap.resolver;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalMavenProject;

/**
 * Factory for Maven and Gradle LocalProject implementations.
 */
public class LocalProjects {
    private static final Logger logger = LoggerFactory.getLogger(LocalProjects.class);
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String POM_XML = "pom.xml";
    
    private Path classesDir;
    private boolean classpathCaching;
    private boolean localProjectsDiscovery;

    private LocalProjects(Path classesDir) {
        this.classesDir = classesDir;
    }
    
    /**
     * Creates new builder, based on the location of classes directory.
     * @param classesDir classes directory.
     * @return LocalProject builder.
     */
    public static LocalProjects dir(Path classesDir) {
        return new LocalProjects(classesDir);
    }
    
    /**
     * Configuring the builder to allow classpath caching.
     */
    public LocalProjects withClasspathCaching(boolean classpathCaching) {
        this.classpathCaching = classpathCaching;
        return this;
    }

    /**
     * Configuring the builder to do local project discovery.
     */
    public LocalProjects withLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    /**
     * Creates LocalProject for the found builder.
     * 
     * @return LocalProject for the found builder.
     * @throws BootstrapException on failure
     */
    public LocalProject build() throws BootstrapException {
        Path p = classesDir;
        while (p != null) {
            if (Files.exists(p.resolve(POM_XML))) {
                logger.info("Creating Maven build info from dir {}", p);
                return classpathCaching || localProjectsDiscovery
                        ? LocalMavenProject.loadWorkspace(p)
                                : LocalMavenProject.load(p);
            }
            if (Files.exists(p.resolve(BUILD_GRADLE))) {
                logger.info("Creating Gradle build info from dir {}", p);
                throw new UnsupportedOperationException("No gradle implementation yet");
            }
            p = p.getParent();
        }
        throw new BootstrapException("Failed to locate builder file for " + classesDir);
    }
}
