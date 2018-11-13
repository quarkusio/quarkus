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
import org.jboss.shamrock.deployment.builditem.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.runtime.graal.ShutdownThread;

/**
 * Shutdown hook processor for native environments.  Such environments do not install signal handlers, so in order
 * to correctly run shutdown hooks on {@code SIGINT}, a handler and thread must be installed.
 */
public class ShutdownHook {

    /**
     * Build step to cause the shutdown thread to be started during init.  This also suffices to ensure that the
     * class is strongly reachable.
     *
     * @return the build item for the shutdown thread class
     */
    @BuildStep
    public RuntimeInitializedClassBuildItem installHook() {
         return new RuntimeInitializedClassBuildItem(ShutdownThread.class.getName());
    }
}
