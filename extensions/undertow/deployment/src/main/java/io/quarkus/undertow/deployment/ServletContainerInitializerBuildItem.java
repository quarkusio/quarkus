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

package io.quarkus.undertow.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public final class ServletContainerInitializerBuildItem extends MultiBuildItem {

    final String sciClass;
    final Set<String> handlesTypes;

    public ServletContainerInitializerBuildItem(String sciClass, Set<String> handlesTypes) {
        this.sciClass = sciClass;
        this.handlesTypes = handlesTypes;
    }

    public String getSciClass() {
        return sciClass;
    }

    public Set<String> getHandlesTypes() {
        return handlesTypes;
    }
}
