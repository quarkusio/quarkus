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

package io.quarkus.deployment.builditem.substrate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A build item that represents a {@link java.lang.reflect.Proxy} definition
 * that will be required on substrate. This definition takes the form of an ordered
 * list of interfaces that this proxy will implement.
 */
public final class SubstrateProxyDefinitionBuildItem extends MultiBuildItem {

    private final List<String> classes;

    public SubstrateProxyDefinitionBuildItem(String... classes) {
        this.classes = Arrays.asList(classes);
    }

    public SubstrateProxyDefinitionBuildItem(List<String> classes) {
        this.classes = new ArrayList<>(classes);
    }

    public List<String> getClasses() {
        return classes;
    }

}
