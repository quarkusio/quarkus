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
public class DependencyOnDifferentVersionsOfAnArtifactInjectingDependenciesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        final TsArtifact deploymentA1 = new TsArtifact("deployment-a", "1");
        install(deploymentA1, "runtime");

        final TsArtifact extensionA1 = TsArtifact.jar("extension-a", "1");
        install(
                extensionA1,
                newJar().addFile(
                        PropsBuilder.init(BootstrapConstants.PROP_INJECT_DEPS, deploymentA1.toString()).build(),
                        BootstrapConstants.DESCRIPTOR_PATH)
                .getPath(),
                true);

        final TsArtifact depA = new TsArtifact("dep-a").addDependency(extensionA1);
        installAsDep(depA);

        final TsArtifact deploymentA2 = new TsArtifact("deployment-a", "2");
        install(deploymentA2);

        final TsArtifact extensionA2 = TsArtifact.jar("extension-a", "2");
        install(
                extensionA2,
                newJar().addFile(
                        PropsBuilder.init(BootstrapConstants.PROP_INJECT_DEPS, deploymentA2.toString()).build(),
                        BootstrapConstants.DESCRIPTOR_PATH)
                .getPath(),
                false);

        final TsArtifact depB = new TsArtifact("dep-b").addDependency(extensionA2);
        installAsDep(depB);
    }
}
