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

package org.jboss.shamrock.bootstrap.resolver.test;

import org.jboss.shamrock.bootstrap.resolver.CollectDependenciesBase;
import org.jboss.shamrock.bootstrap.resolver.TsArtifact;
import org.jboss.shamrock.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class OnlyDirectTestScopeDependenciesAreCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        installAsDep(new TsDependency(new TsArtifact("required-dep-a"), "compile"), true);
        installAsDep(new TsDependency(new TsArtifact("required-dep-b"), "runtime"), true);

        // the reason it is collected in compile scope is because required-dep-c needs it, although a different version
        final TsArtifact common1 = install(
                new TsArtifact("common", "1")
                .addDependency(new TsDependency(new TsArtifact("not-collected"), "test")),
                true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("direct-test-dep")
                        .addDependency(common1),
                        "test"),
                true);


        installAsDep(
                new TsArtifact("required-dep-c")
                .addDependency(new TsArtifact("common", "2")),
                true);
    }
}
