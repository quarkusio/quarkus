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

package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import io.quarkus.runtime.StartupEvent;

/**
 * A symbolic class that represents a service start.
 * <p>
 * {@link StartupEvent} is fired after all services are started.
 */
public final class ServiceStartBuildItem extends MultiBuildItem {

    private final String name;

    public ServiceStartBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
