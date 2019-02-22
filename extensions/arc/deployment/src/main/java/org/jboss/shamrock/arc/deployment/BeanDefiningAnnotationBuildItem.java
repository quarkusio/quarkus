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
 * This build item is used to specify additional bean defining annotations. See also
 * <a href="http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#bean_defining_annotations">2.5.1. Bean defining annotations</a>.
 * <p>
 * By default, the resulting beans must not be removed even if they are considered unused and
 * {@link ArcConfig#removeUnusedBeans} is enabled.
 */
public final class BeanDefiningAnnotationBuildItem extends MultiBuildItem {

    private final DotName name;
    private final DotName defaultScope;
    private final boolean removable;

    public BeanDefiningAnnotationBuildItem(DotName name) {
        this(name, null);
    }

    public BeanDefiningAnnotationBuildItem(DotName name, DotName defaultScope) {
        this(name, defaultScope, false);
    }

    public BeanDefiningAnnotationBuildItem(DotName name, DotName defaultScope, boolean removable) {
        this.name = name;
        this.defaultScope = defaultScope;
        this.removable = removable;
    }

    public DotName getName() {
        return name;
    }

    public DotName getDefaultScope() {
        return defaultScope;
    }

    /**
     * 
     * @return true if the resulting beans should be removed if they're considered unused as described in
     *         {@link ArcConfig#removeUnusedBeans}
     */
    public boolean isRemovable() {
        return removable;
    }

}
