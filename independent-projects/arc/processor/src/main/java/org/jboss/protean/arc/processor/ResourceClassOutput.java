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

package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.arc.processor.ResourceOutput.Resource.SpecialType;
import org.jboss.protean.gizmo.ClassOutput;

/**
 *
 * @author Martin Kouba
 */
public class ResourceClassOutput implements ClassOutput {

    private final List<Resource> resources = new ArrayList<>();

    private final boolean applicationClass;

    private final Function<String, SpecialType> specialTypeFunction;

    public ResourceClassOutput(boolean applicationClass) {
        this(applicationClass, null);
    }

    public ResourceClassOutput(boolean applicationClass, Function<String, SpecialType> specialTypeFunction) {
        this.applicationClass = applicationClass;
        this.specialTypeFunction = specialTypeFunction;
    }

    @Override
    public void write(String name, byte[] data) {
        resources.add(ResourceImpl.javaClass(name, data, specialTypeFunction != null ? specialTypeFunction.apply(name) : null, applicationClass));
    }

    List<Resource> getResources() {
        return resources;
    }

}
