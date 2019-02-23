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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a static resource should be included in the native image
 */
public final class SubstrateResourceBuildItem extends MultiBuildItem {

    private final List<String> resources;

    public SubstrateResourceBuildItem(String... resources) {
        this.resources = Arrays.asList(resources);
    }

    public SubstrateResourceBuildItem(List<String> resources) {
        this.resources = new ArrayList<>(resources);
    }

    public List<String> getResources() {
        return resources;
    }
}
