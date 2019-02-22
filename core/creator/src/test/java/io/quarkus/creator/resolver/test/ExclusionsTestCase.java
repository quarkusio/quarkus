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

package io.quarkus.creator.resolver.test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ExclusionsTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact requiredTransitive = new TsArtifact("required-transitive")
                .addDependency(
                        new TsArtifact("excluded-dep", "2")
                                .addDependency(new TsArtifact("other-dep")));
        install(requiredTransitive, true);

        final TsArtifact otherDep2 = new TsArtifact("other-dep", "2");
        install(otherDep2, true);

        final TsArtifact otherRequiredTransitive = new TsArtifact("other-required-transitive")
                .addDependency(otherDep2);
        install(otherRequiredTransitive, true);

        installAsDep(
                new TsArtifact("required-dep1")
                        .addDependency(
                                new TsDependency(requiredTransitive)
                                        .exclude("excluded-dep"))
                        .addDependency(otherRequiredTransitive));
    }
}
