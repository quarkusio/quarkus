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

package org.jboss.shamrock.deployment.cdi;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.shamrock.deployment.Capabilities;

/**
 * This build item is used to specify additional bean defining annotations. See also
 * <a href="http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#bean_defining_annotations">2.5.1. Bean defining annotations</a>.
 *
 * @see Capabilities#CDI_ARC
 */
public final class BeanDefiningAnnotationBuildItem extends MultiBuildItem {

    private final DotName name;
    private final DotName defaultScope;

    public BeanDefiningAnnotationBuildItem(DotName name) {
        this(name, null);
    }

    public BeanDefiningAnnotationBuildItem(DotName name, DotName defaultScope) {
        this.name = name;
        this.defaultScope = defaultScope;
    }

    public DotName getName() {
        return name;
    }

    public DotName getDefaultScope() {
        return defaultScope;
    }
}
