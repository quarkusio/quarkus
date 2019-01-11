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

package org.jboss.shamrock.deployment;

import java.util.Set;

import org.jboss.builder.item.SimpleBuildItem;

/**
 * The list of capabilities.
 *
 */
public final class Capabilities extends SimpleBuildItem {

    public static final String CDI_ARC = "org.jboss.shamrock.cdi";
    public static final String TRANSACTIONS = "org.jboss.shamrock.transactions";

    private final Set<String> capabilities;

    public boolean isCapabilityPresent(String capability) {
        return capabilities.contains(capability);
    }

    public Capabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }
    
}
