/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.dev;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.builditem.ServiceStartBuildItem;

public class HotDeploymentConfigFileBuildStep {

    @BuildStep
    ServiceStartBuildItem setupConfigFileHotDeployment(List<HotDeploymentConfigFileBuildItem> files) {
        //TODO: this should really be an output of the RuntimeRunner
        Set<String> fileSet = files.stream().map(HotDeploymentConfigFileBuildItem::getLocation).collect(Collectors.toSet());
        RuntimeUpdatesProcessor processor = DevModeMain.runtimeUpdatesProcessor;
        if (processor != null) {
            processor.setConfigFilePaths(fileSet);
        }
        return null;
    }
}
