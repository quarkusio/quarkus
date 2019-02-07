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

/**
 *
 * @author Alexey Loubyansky
 */
public class TransitiveVersionOverridesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x1 = new TsArtifact("x", "2");

        final TsArtifact c = new TsArtifact("c")
                .addDependency(x1);
        install(c, true);

        final TsArtifact b = new TsArtifact("b").addDependency(c);
        install(b, true);

        installAsDep(new TsArtifact("a")
                .addDependency(b));

        final TsArtifact x2 = new TsArtifact("x", "1");
        install(x2, true);

        final TsArtifact z = new TsArtifact("z").addDependency(x2);
        install(z, true);

        installAsDep(new TsArtifact("y")
                .addDependency(z));
    }
}
