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

import java.util.List;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppDependency;

/**
 * Provides access to information from builders such as Maven and Gradle.
 */
public interface BuilderInfo extends AutoCloseable {
    /**
     * Configuring the builder to allow classpath caching.
     */
    BuilderInfo withClasspathCaching(boolean classpathCaching);

    /**
     * Configuring the builder to do local project discovery.
     */
    BuilderInfo withLocalProjectsDiscovery(boolean localProjectsDiscovery);

    LocalProject getLocalProject() throws BootstrapException;

    List<AppDependency> getDeploymentDependencies(boolean offline)
            throws BootstrapDependencyProcessingException, AppModelResolverException;

    @Override
    void close() throws BootstrapException;
}
