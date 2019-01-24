/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.shamrock.creator.resolver.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.shamrock.creator.AppDependency;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CollectDependenciesBase extends ResolverSetupCleanup {

    protected TsArtifact root;
    protected List<AppDependency> expectedResult = Collections.emptyList();

    @Override
    public void setup() throws Exception {
        super.setup();
        root = new TsArtifact("root");
        setupDependencies();
    }

    protected abstract void setupDependencies();

    @Test
    public void testCollectedDependencies() throws Exception {
        install(root);
        final List<AppDependency> resolvedDeps = resolver.collectDependencies(root.toAppArtifact());
        assertEquals(expectedResult, resolvedDeps);
    }

    protected void install(TsArtifact dep, boolean collected) {
        install(dep);
        if(collected) {
            addCollectedDep(dep);
        }
    }

    protected void installAsDep(TsArtifact dep) {
        installAsDep(dep, true);
    }

    protected void installAsDep(TsArtifact dep, boolean collected) {
        installAsDep(new TsDependency(dep), collected);
    }

    protected void installAsDep(TsDependency dep) {
        installAsDep(dep, true);
    }

    protected void installAsDep(TsDependency dep, boolean collected) {
        final TsArtifact artifact = dep.artifact;
        install(artifact);
        root.addDependency(dep);
        if(!collected) {
            return;
        }
        addCollectedDep(artifact, dep.scope == null ? "compile" : dep.scope);
    }

    protected void addCollectedDep(final TsArtifact artifact) {
        addCollectedDep(artifact, "compile");
    }

    protected void addCollectedDep(final TsArtifact artifact, final String scope) {
        if(expectedResult.isEmpty()) {
            expectedResult = new ArrayList<>();
        }
        expectedResult.add(new AppDependency(artifact.toAppArtifact(), scope));
    }
}
