/*
 * Copyright 2016-2019 Red Hat, Inc. and/or its affiliates
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

package org.jboss.shamrock.creator.resolver;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jboss.shamrock.creator.AppCreatorException;
import org.jboss.shamrock.creator.AppDependency;
import org.jboss.shamrock.creator.resolver.aether.AetherArtifactResolver;
import org.jboss.shamrock.creator.util.IoUtils;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolverSetupCleanup {

    protected Path workDir;
    protected Path repoHome;
    protected AetherArtifactResolver resolver;
    protected TsRepoBuilder repo;

    @Before
    public void setup() throws Exception {
        workDir = IoUtils.createRandomTmpDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));
        //resolver = AetherArtifactResolver.getInstance(repoHome, Collections.emptyList());
        resolver = AetherArtifactResolver.getBootstrapResolver(repoHome, Collections.emptyList());
        repo = TsRepoBuilder.getInstance(resolver, workDir);
    }

    @After
    public void cleanup() {
        if(workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    protected TsJar newJar() {
        return new TsJar(workDir.resolve(UUID.randomUUID().toString()));
    }

    protected TsArtifact install(TsArtifact artifact) {
        repo.install(artifact);
        return artifact;
    }

    protected TsArtifact install(TsArtifact artifact, Path p) {
        repo.install(artifact, p);
        return artifact;
    }

    protected List<AppDependency> collectDeps(TsArtifact artifact) throws AppCreatorException {
        return resolver.collectDependencies(artifact.toAppArtifact());
    }
}
