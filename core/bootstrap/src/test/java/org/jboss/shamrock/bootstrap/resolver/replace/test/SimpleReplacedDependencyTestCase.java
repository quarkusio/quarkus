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

package org.jboss.shamrock.bootstrap.resolver.replace.test;

import org.jboss.shamrock.bootstrap.BootstrapConstants;
import org.jboss.shamrock.bootstrap.resolver.CollectDependenciesBase;
import org.jboss.shamrock.bootstrap.resolver.PropsBuilder;
import org.jboss.shamrock.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleReplacedDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        final TsArtifact extension = TsArtifact.jar("extension");
        final TsArtifact deployment = new TsArtifact("deployment").addDependency(extension);

        installAsDep(
                extension,
                newJar().addFile(
                        PropsBuilder.init(BootstrapConstants.PROP_REPLACE_WITH_DEP, deployment.toString()).build(),
                        BootstrapConstants.DESCRIPTOR_PATH)
                .getPath(),
                true);

        install(deployment, true);
    }
}
