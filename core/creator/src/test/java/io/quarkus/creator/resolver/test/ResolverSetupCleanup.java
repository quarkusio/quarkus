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

package io.quarkus.creator.resolver.test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;
import io.quarkus.creator.resolver.aether.AetherArtifactResolver;
import io.quarkus.creator.util.IoUtils;

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
        resolver = AetherArtifactResolver.getInstance(repoHome, Collections.emptyList());
        repo = TsRepoBuilder.getInstance(resolver, workDir);
    }

    @After
    public void cleanup() {
        if (workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    protected TsArtifact install(TsArtifact artifact) {
        repo.install(artifact);
        return artifact;
    }

    protected List<AppDependency> collectDeps(TsArtifact artifact) throws AppCreatorException {
        return resolver.collectDependencies(artifact.toAppArtifact());
    }
}
