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

package org.jboss.shamrock.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A generated CDI bean. If this is produced then a {@link org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem}
 * should not be produced for the same class, as Arc will take care of this.
 */
public final class GeneratedBeanBuildItem extends MultiBuildItem {

    final String name;
    final byte[] data;

    public GeneratedBeanBuildItem(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
