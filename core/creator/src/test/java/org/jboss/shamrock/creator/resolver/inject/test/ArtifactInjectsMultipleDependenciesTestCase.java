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

package org.jboss.shamrock.creator.resolver.inject.test;

import java.io.BufferedWriter;
import java.io.StringWriter;
import org.jboss.shamrock.bootstrap.BootstrapConstants;
import org.jboss.shamrock.creator.resolver.CollectDependenciesBase;
import org.jboss.shamrock.creator.resolver.PropsBuilder;
import org.jboss.shamrock.creator.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArtifactInjectsMultipleDependenciesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        final TsArtifact injectedDepA = new TsArtifact("injected-dep-a");
        install(injectedDepA, "runtime");

        final TsArtifact injectedDepB = new TsArtifact("injected-dep-b");
        install(injectedDepB, "runtime");

        final TsArtifact injectedDepC = new TsArtifact("injected-dep-c");
        install(injectedDepC, "runtime");

        final TsArtifact injectedDepD = new TsArtifact("injected-dep-d");
        install(injectedDepD, "runtime");

        final StringWriter strWriter = new StringWriter();
        try(BufferedWriter writer = new BufferedWriter(strWriter)) {
            writer.write(injectedDepA.toString() + ',' + injectedDepB.toString() + " , " + injectedDepC.toString());
            writer.newLine();
            writer.write("\t\t\t" + injectedDepD);
        }
        installAsDep(
                TsArtifact.jar("direct-dep"),
                newJar().addFile(
                        PropsBuilder.init(
                                BootstrapConstants.PROP_INJECT_DEPS,
                                strWriter.getBuffer().toString())
                        .build(),
                        BootstrapConstants.DESCRIPTOR_PATH)
                .getPath(),
                true);
    }
}
