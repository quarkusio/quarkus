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

package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.ExecutorTemplate;
import io.quarkus.runtime.ThreadPoolConfig;

/**
 *
 */
public class ThreadPoolSetup {

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    public ExecutorBuildItem createExecutor(ExecutorTemplate setupTemplate, ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            ThreadPoolConfig threadPoolConfig) {
        return new ExecutorBuildItem(
                setupTemplate.setupRunTime(shutdownContextBuildItem, threadPoolConfig, launchModeBuildItem.getLaunchMode()));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem registerClasses() {
        // make sure that the config provider gets initialized only at run time
        return new RuntimeInitializedClassBuildItem(ExecutorTemplate.class.getName());
    }
}
