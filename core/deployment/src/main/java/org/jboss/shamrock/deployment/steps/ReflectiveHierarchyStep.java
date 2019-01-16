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

package org.jboss.shamrock.deployment.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.UnresolvedTypeVariable;
import org.jboss.jandex.VoidType;
import org.jboss.logging.Logger;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.BuildProducer;
import javax.inject.Inject;

import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;

public class ReflectiveHierarchyStep {

    private static final Logger log = Logger.getLogger(ReflectiveHierarchyStep.class);

    @Inject
    List<ReflectiveHierarchyBuildItem> hierarchy;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    public void build() throws Exception {
        Set<DotName> processedReflectiveHierarchies = new HashSet<>();
        for (ReflectiveHierarchyBuildItem i : hierarchy) {
            addReflectiveHierarchy(i.getType(), processedReflectiveHierarchies);
        }
    }

    private void addReflectiveHierarchy(Type type, Set<DotName> processedReflectiveHierarchies) {
        if (type instanceof VoidType ||
                type instanceof PrimitiveType ||
                type instanceof UnresolvedTypeVariable) {
            return;
        } else if (type instanceof ClassType) {
            if (skipClass(type.name(), processedReflectiveHierarchies)) {
                return;
            }

            addClassTypeHierarchy(type.name(), processedReflectiveHierarchies);

            for (ClassInfo subclass : combinedIndexBuildItem.getIndex().getAllKnownSubclasses(type.name())) {
                addClassTypeHierarchy(subclass.name(), processedReflectiveHierarchies);
            }
            for (ClassInfo subclass : combinedIndexBuildItem.getIndex().getAllKnownImplementors(type.name())) {
                addClassTypeHierarchy(subclass.name(), processedReflectiveHierarchies);
            }
        } else if (type instanceof ArrayType) {
            addReflectiveHierarchy(type.asArrayType().component(), processedReflectiveHierarchies);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) type;
            addReflectiveHierarchy(p.owner(), processedReflectiveHierarchies);
            for (Type arg : p.arguments()) {
                addReflectiveHierarchy(arg, processedReflectiveHierarchies);
            }
        }
    }

    private void addClassTypeHierarchy(DotName name, Set<DotName> processedReflectiveHierarchies) {
        if (skipClass(name, processedReflectiveHierarchies)) {
            return;
        }
        processedReflectiveHierarchies.add(name);
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, name.toString()));
        ClassInfo info = combinedIndexBuildItem.getIndex().getClassByName(name);
        if (info == null) {
            log.warn("Unable to find annotation info for " + name
                    + ", either it should be added to the Jandex index or it might be incorrectly registered for reflection.");
        } else {
            addClassTypeHierarchy(info.superName(), processedReflectiveHierarchies);
            for (FieldInfo i : info.fields()) {
                addReflectiveHierarchy(i.type(), processedReflectiveHierarchies);
            }
            for (MethodInfo method : info.methods()) {
                // we only add the return types of the potential getters
                if (method.parameters().size() == 0) {
                    addReflectiveHierarchy(method.returnType(), processedReflectiveHierarchies);
                }
            }
        }
    }

    private boolean skipClass(DotName name, Set<DotName> processedReflectiveHierarchies) {
        return name.toString().startsWith("java.") || processedReflectiveHierarchies.contains(name);
    }
}
