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

package io.quarkus.deployment.builditem.substrate;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A class that will be reinitialized at runtime by Substrate. This will result in the static
 * initializer running twice.
 */
public final class RuntimeReinitializedClassBuildItem extends MultiBuildItem {

    private final String className;

    public RuntimeReinitializedClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
