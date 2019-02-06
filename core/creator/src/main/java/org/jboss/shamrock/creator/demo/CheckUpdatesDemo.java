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

import org.jboss.shamrock.bootstrap.resolver.AppDependency;
import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.phase.curate.CurateOutcome;
import org.jboss.shamrock.creator.phase.curate.CuratePhase;
import org.jboss.shamrock.creator.phase.curate.VersionUpdate;
import org.jboss.shamrock.creator.phase.curate.VersionUpdateNumber;

/**
 *
 * @author Alexey Loubyansky
 */
public class CheckUpdatesDemo extends ConfigDemoBase {

    public static void main(String[] args) throws Exception {
        new CheckUpdatesDemo().run();
    }

    @Override
    protected void initProps(Properties props) {
        props.setProperty(CuratePhase.completePropertyName(CuratePhase.CONFIG_PROP_VERSION_UPDATE), VersionUpdate.NONE.getName()); // NONE, next, latest
        props.setProperty(CuratePhase.completePropertyName(CuratePhase.CONFIG_PROP_VERSION_UPDATE_NUMBER), VersionUpdateNumber.MINOR.getName()); // major, minor, MICRO
    }

    @Override
    protected boolean isLogLibDiff() {
        return false;
    }

    @Override
    public void demo(AppCreator creator) throws Exception {
        final CurateOutcome curate = creator.resolveOutcome(CurateOutcome.class);
        final boolean updatesAvailable = curate.hasUpdatedDeps();
        System.out.println("Updates available: " + updatesAvailable);
        if(!updatesAvailable) {
            return;
        }
        System.out.println("Available updates:");
        for(AppDependency dep : curate.getUpdatedDeps()) {
            System.out.println("- " + dep);
        }
    }
}
