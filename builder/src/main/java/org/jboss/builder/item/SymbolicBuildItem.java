/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.builder.item;

/**
 * The symbolic build item.
 */
public final class SymbolicBuildItem extends NamedBuildItem<Enum<?>> {

    private static final SymbolicBuildItem INSTANCE = new SymbolicBuildItem();

    private SymbolicBuildItem() {
    }

    /**
     * Get the singleton instance.
     *
     * @return the singleton instance (not {@code null})
     */
    public static SymbolicBuildItem getInstance() {
        return INSTANCE;
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(final Object obj) {
        return obj == this;
    }

    public String toString() {
        return "symbolic";
    }
}
