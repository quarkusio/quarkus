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

package io.quarkus.arc.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 *
 */
@Dependent
public class LifecycleEventRunner {

    @Inject
    Event<StartupEvent> startup;

    @Inject
    Event<ShutdownEvent> shutdown;

    public void fireStartupEvent() {
        startup.fire(new StartupEvent());
    }

    public void fireShutdownEvent() {
        shutdown.fire(new ShutdownEvent());
    }

}
