/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.jboss.shamrock.creator.demo;

import java.util.Properties;

import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.phase.curate.CuratePhase;
import org.jboss.shamrock.creator.phase.curate.VersionUpdate;
import org.jboss.shamrock.creator.phase.curate.VersionUpdateNumber;
import org.jboss.shamrock.creator.phase.runnerjar.RunnerJarOutcome;

/**
 *
 * @author Alexey Loubyansky
 */
public class UpdateToNextVersionDemo extends ConfigDemoBase {

    public static void main(String[] args) throws Exception {
        new UpdateToNextVersionDemo().run();
    }
/*
    @Override
    protected Path initAppJar() {
        final Path shamrockRoot = Paths.get("").toAbsolutePath().getParent().getParent();
        final Path quickstartsRoot = shamrockRoot.getParent().resolve("protean-quickstarts");
        if(!Files.exists(quickstartsRoot)) {
            throw new IllegalStateException("Failed to locate protean-quickstarts repo at " + quickstartsRoot);
        }
        final Path appDir = quickstartsRoot.resolve("input-validation").resolve("target");
        return appDir.resolve("input-validation-1.0-SNAPSHOT.jar");
    }
*/
    @Override
    protected void initProps(Properties props) {
        props.setProperty(CuratePhase.completePropertyName(CuratePhase.CONFIG_PROP_VERSION_UPDATE), VersionUpdate.NEXT.getName());
        props.setProperty(CuratePhase.completePropertyName(CuratePhase.CONFIG_PROP_VERSION_UPDATE_NUMBER), VersionUpdateNumber.MINOR.getName());
    }

    @Override
    public void demo(AppCreator creator) throws Exception {
        creator.resolveOutcome(RunnerJarOutcome.class);
    }
}
