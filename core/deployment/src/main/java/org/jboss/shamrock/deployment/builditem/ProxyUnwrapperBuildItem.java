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

package org.jboss.shamrock.deployment.builditem;

import java.util.function.Function;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A build item that can be used to unwrap CDI or other proxies
 */
public final class ProxyUnwrapperBuildItem extends MultiBuildItem {

    private final Function<Object, Object> unwrapper;

    public ProxyUnwrapperBuildItem(Function<Object, Object> unwrapper) {
        this.unwrapper = unwrapper;
    }

    public Function<Object, Object> getUnwrapper() {
        return unwrapper;
    }
}
