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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

public final class AdditionalBeanBuildItem extends MultiBuildItem {

    private final List<String> beanNames;

    public AdditionalBeanBuildItem(String... beanNames) {
        this.beanNames = Arrays.asList(beanNames);
    }

    public AdditionalBeanBuildItem(Class<?>... beanClasss) {
        beanNames = new ArrayList<>(beanClasss.length);
        for (Class<?> i : beanClasss) {
            beanNames.add(i.getName());
        }
    }

    public List<String> getBeanNames() {
        return Collections.unmodifiableList(beanNames);
    }
}
