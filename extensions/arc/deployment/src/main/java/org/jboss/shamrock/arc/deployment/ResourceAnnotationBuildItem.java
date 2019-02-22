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

package io.quarkus.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;

/**
 * This build item is used to specify resource annotations that makes it possible to resolve non-CDI injection points, such as
 * Java EE resources.
 */
public final class ResourceAnnotationBuildItem extends MultiBuildItem {

    private final DotName name;

    public ResourceAnnotationBuildItem(DotName name) {
        this.name = name;
    }

    public DotName getName() {
        return name;
    }
}
