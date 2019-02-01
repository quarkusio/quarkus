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

package org.jboss.shamrock.undertow;

import java.util.ArrayList;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

import io.undertow.servlet.ServletExtension;
import org.jboss.shamrock.runtime.ObjectSubstitution;

public final class ServletExtensionBuildItem extends MultiBuildItem {

    private final ServletExtension value;
    private final List<Class<? extends ObjectSubstitution<?, ?>>> objSubstitutions = new ArrayList<>();

    public ServletExtensionBuildItem(ServletExtension value) {
        this.value = value;
    }

    public ServletExtension getValue() {
        return value;
    }

    public void addObjectSubstitution(Class<? extends ObjectSubstitution<?, ?>> sub) {
        objSubstitutions.add(sub);
    }
    public List<Class<? extends ObjectSubstitution<?, ?>>> getObjSubstitutions() {
        return objSubstitutions;
    }
}
