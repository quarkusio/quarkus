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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.builder.item.MultiBuildItem;

/**
 * This build item is used to specify additional bean classes to be analyzed.
 * <p>
 * By default, the resulting beans may be removed if they are considered unused and {@link ArcConfig#removeUnusedBeans} is enabled. 
 */
public final class AdditionalBeanBuildItem extends MultiBuildItem {

    private final List<String> beanClasses;
    private final boolean removable;
    
    public AdditionalBeanBuildItem(String... beanClasses) {
        this(true, beanClasses);
    }
    
    public AdditionalBeanBuildItem(boolean removable, String... beanClasses) {
        this(Arrays.asList(beanClasses), removable);
    }

    public AdditionalBeanBuildItem(Class<?>... beanClasses) {
        this(true, beanClasses);
    }
    
    public AdditionalBeanBuildItem(boolean removable, Class<?>... beanClasses) {
        this(Arrays.stream(beanClasses).map(Class::getName).collect(Collectors.toList()), removable);
    }
    
    AdditionalBeanBuildItem(List<String> beanClasses, boolean removable) {
        this.beanClasses = beanClasses;
        this.removable = removable;
    }

    public List<String> getBeanClasses() {
        return Collections.unmodifiableList(beanClasses);
    }

    public boolean isRemovable() {
        return removable;
    }
    
}
