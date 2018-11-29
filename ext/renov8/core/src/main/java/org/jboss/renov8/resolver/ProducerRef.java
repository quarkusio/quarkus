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

package org.jboss.renov8.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.renov8.Pack;
import org.jboss.renov8.PackLocation;

/**
 *
 * @author Alexey Loubyansky
 */
class ProducerRef<P extends Pack> {

    static final int ORDERED    = 0b001;
    static final int UPDATED    = 0b010;
    static final int VISITED    = 0b100;

    final PackLocation location;
    private P spec;
    private List<ProducerRef<P>> deps = Collections.emptyList();
    private int status;

    protected ProducerRef(PackLocation location, int status) {
        this.location = location;
        this.status = status;
    }

    void setSpec(P spec) {
        this.spec = spec;
        final int depsTotal = spec.getDependencies().size();
        if(depsTotal > 0) {
            deps = new ArrayList<ProducerRef<P>>(depsTotal);
            for(int i = 0; i < depsTotal; ++i) {
                deps.add(null);
            }
        }
    }

    P getSpec() {
        return spec;
    }

    boolean isFlagOn(int flag) {
        return (status & flag) > 0;
    }

    boolean setFlag(int flag) {
        if((status & flag) > 0) {
            return false;
        }
        status |= flag;
        return true;
    }

    void clearFlag(int flag) {
        if((status & flag) > 0) {
            status ^= flag;
        }
    }

    boolean isDependencySet(int i) {
        return deps.get(i) != null;
    }

    void setDependency(int i, ProducerRef<P> dep) {
        deps.set(i, dep);
    }

    List<ProducerRef<P>> getDependencies() {
        return deps;
    }

    boolean hasDependencies() {
        return !deps.isEmpty();
    }

    boolean isLoaded() {
        return spec != null;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(location);
        if(spec != null) {
            buf.append(" spec=").append(spec);
        }
        return buf.toString();
    }
}
