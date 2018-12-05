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
package org.jboss.shamrock.arc.test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.StartupEvent;

@ApplicationScoped
public class SimpleBean {
    
    static final String DEFAULT = "bar";

    private final AtomicReference<StartupEvent> startupEvent = new AtomicReference<StartupEvent>();

    @Inject
    @ConfigProperty(name = "unconfigured", defaultValue = DEFAULT)
    String foo;

    @Inject
    @ConfigProperty(name = "unconfigured")
    Optional<String> fooOptional;

    void onStart(@Observes StartupEvent event) {
        startupEvent.set(event);
    }

    AtomicReference<StartupEvent> getStartupEvent() {
        return startupEvent;
    }

    String getFoo() {
        return foo;
    }

    Optional<String> getFooOptional() {
        return fooOptional;
    }
    
}


