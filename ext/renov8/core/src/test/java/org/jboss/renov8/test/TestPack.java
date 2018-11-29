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

package org.jboss.renov8.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.renov8.Pack;
import org.jboss.renov8.PackLocation;
import org.jboss.renov8.config.PackConfig;
import org.jboss.renov8.utils.StringUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class TestPack implements Pack {

    public static class Builder {

        protected PackLocation location;
        protected List<PackConfig> deps = new ArrayList<>(0);

        protected Builder() {
        }

        public Builder setLocation(PackLocation location) {
            this.location = location;
            return this;
        }

        public Builder addDependency(PackLocation dep) {
            return addDependency(PackConfig.forLocation(dep));
        }

        public Builder addDependency(PackConfig dep) {
            deps.add(dep);
            return this;
        }

        public TestPack build() {
            return new TestPack(this);
        }
    }

    public static Builder builder(PackLocation location) {
        return builder().setLocation(location);
    }

    public static Builder builder() {
        return new Builder();
    }

    protected final PackLocation location;
    protected final List<PackConfig> deps;

    protected TestPack(Builder builder) {
        this.location = builder.location;
        this.deps = builder.deps.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(builder.deps);
    }

    /* (non-Javadoc)
     * @see org.jboss.renov8.spec.PackSpec#getLocation()
     */
    @Override
    public PackLocation getLocation() {
        return location;
    }

    /* (non-Javadoc)
     * @see org.jboss.renov8.spec.PackSpec#hasDependencies()
     */
    @Override
    public boolean hasDependencies() {
        return !deps.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.renov8.spec.PackSpec#getDependencies()
     */
    @Override
    public List<PackConfig> getDependencies() {
        return deps;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deps == null) ? 0 : deps.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
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
        TestPack other = (TestPack) obj;
        if (deps == null) {
            if (other.deps != null)
                return false;
        } else if (!deps.equals(other.deps))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(location);
        if(!deps.isEmpty()) {
            buf.append(" deps=");
            StringUtils.append(buf, deps);
        }
        return buf.append(']').toString();
    }
}
