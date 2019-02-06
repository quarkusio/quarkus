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

package org.jboss.shamrock.bootstrap.resolver.inject.test;

import org.jboss.shamrock.bootstrap.BootstrapConstants;
import org.jboss.shamrock.bootstrap.resolver.CollectDependenciesBase;
import org.jboss.shamrock.bootstrap.resolver.PropsBuilder;
import org.jboss.shamrock.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class InjectedDependenciesDependingOnDifferentVersionsOfCommonDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        final TsArtifact transitive1 = new TsArtifact("transitive1");
        install(transitive1, "runtime");

        final TsArtifact transitive2 = new TsArtifact("transitive2").addDependency(transitive1);
        install(transitive2, "runtime");

        final TsArtifact common1 = new TsArtifact("common", "1");
        install(common1, "runtime");

        final TsArtifact deploymentA = new TsArtifact("deployment-a")
                .addDependency(transitive2)
                .addDependency(common1);
        install(deploymentA, "runtime");

        installAsDep(
                TsArtifact.jar("extension-a"),
                newJar().addFile(
                        PropsBuilder.init(BootstrapConstants.PROP_INJECT_DEPS, deploymentA.toString()).build(),
                        BootstrapConstants.DESCRIPTOR_PATH)
                .getPath(),
                true);

        final TsArtifact transitive1_2 = new TsArtifact("transitive1", "2");
        install(transitive1_2);

        final TsArtifact transitive4 = new TsArtifact("transitive4").addDependency(transitive1_2);
        install(transitive4, "runtime");

        final TsArtifact common2 = new TsArtifact("common", "2");
        install(common2);

        final TsArtifact deploymentB = new TsArtifact("deployment-b")
                .addDependency(common2)
                .addDependency(transitive4);
        install(deploymentB, "runtime");

        installAsDep(
                TsArtifact.jar("extension-b"),
                newJar().addFile(
                        PropsBuilder.init(BootstrapConstants.PROP_INJECT_DEPS, deploymentB.toString()).build(),
                        BootstrapConstants.DESCRIPTOR_PATH)
                .getPath(),
                true);
    }
}
