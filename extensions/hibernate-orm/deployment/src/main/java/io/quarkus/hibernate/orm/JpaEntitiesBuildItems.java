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

package io.quarkus.hibernate.orm;

import java.util.HashSet;
import java.util.Set;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * Internal model to represent which objects are likely needing enhancement
 * via HibernateEntityEnhancer.
 */
public final class JpaEntitiesBuildItems extends SimpleBuildItem {

    private final Set<String> classNames = new HashSet<String>();

    void addEntity(final String className) {
        classNames.add(className);
    }

    void registerAllForReflection(final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (String className : classNames) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
        }
    }

    public Set<String> getClassNames() {
        return classNames;
    }
}
