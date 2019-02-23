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
public class OptionalDepsNotCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        installAsDep(new TsDependency(new TsArtifact("required-dep-a"), "compile"), true);
        installAsDep(new TsDependency(new TsArtifact("required-dep-b"), "runtime"), true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("optional-dep")
                                .addDependency(new TsArtifact("common", "1")),
                        true),
                false);

        final TsArtifact common2 = new TsArtifact("common", "2");
        install(common2, true);

        installAsDep(new TsArtifact("required-dep-c")
                .addDependency(common2), true);
    }
}
