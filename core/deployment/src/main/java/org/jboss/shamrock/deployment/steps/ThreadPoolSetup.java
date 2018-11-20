/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.shamrock.deployment.steps;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.ExecutorBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.runtime.ExecutorTemplate;

/**
 */
public class ThreadPoolSetup {

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    public ExecutorBuildItem createExecutor(ExecutorTemplate setupTemplate) {
        return new ExecutorBuildItem(setupTemplate.setupRunTime(
            // build time default config constants - static method calls are not proxied
            ExecutorTemplate.getIntConfigVal(ExecutorTemplate.CORE_POOL_SIZE, -1),
            ExecutorTemplate.getIntConfigVal(ExecutorTemplate.MAX_POOL_SIZE, -1),
            ExecutorTemplate.getIntConfigVal(ExecutorTemplate.QUEUE_SIZE, Integer.MAX_VALUE),
            ExecutorTemplate.getFloatConfigVal(ExecutorTemplate.GROWTH_RESISTANCE, 0f),
            ExecutorTemplate.getIntConfigVal(ExecutorTemplate.KEEP_ALIVE_MILLIS, 60_000)
        ));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem registerClasses() {
        // make sure that the config provider gets initialized only at run time
        return new RuntimeInitializedClassBuildItem(ExecutorTemplate.class.getName());
    }
}
