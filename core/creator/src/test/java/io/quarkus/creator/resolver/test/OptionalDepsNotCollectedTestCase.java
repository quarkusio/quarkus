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

package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class OnlyDirectOptionalDepsAreCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        installAsDep(new TsDependency(new TsArtifact("required-dep-a"), "compile"), true);
        installAsDep(new TsDependency(new TsArtifact("required-dep-b"), "runtime"), true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("optional-dep")
                        .addDependency(new TsDependency(new TsArtifact("common", "1"), true))
                        .addDependency(new TsDependency(new TsArtifact("other", "1"), true)),
                        true),
                true);

        TsArtifact common2 = install(new TsArtifact("common", "2"), true);
        installAsDep(new TsArtifact("required-dep-c")
                .addDependency(common2), true);
    }
}
