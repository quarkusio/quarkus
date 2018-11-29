/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8.config;

import org.jboss.renov8.PackLocation;

/**
 * This class represents a dependency on a pack.
 * Dependencies can be direct or transitive.
 * Transitive dependencies normally won't be expressed in the original
 * configuration. They exist only to be able to override a transitive dependency
 * versions and still handle them as transitive during updates (which mainly means
 * that they may go away with the next version update of the direct dependencies).
 *
 * @author Alexey Loubyansky
 */
public class PackConfig {

    public static PackConfig forLocation(PackLocation pl) {
        return new PackConfig(pl, false);
    }

    public static PackConfig forTransitive(PackLocation pl) {
        return new PackConfig(pl, true);
    }

    private final PackLocation location;
    private final boolean transitive;

    protected PackConfig(PackLocation location, boolean transitive) {
        this.location = location;
        this.transitive = transitive;
    }

    public PackLocation getLocation() {
        return location;
    }

    public boolean isTransitive() {
        return transitive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + (transitive ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PackConfig other = (PackConfig) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (transitive != other.transitive)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(location);
        if(transitive) {
            buf.append(" transitive");
        }
        return buf.append(']').toString();
    }
}
