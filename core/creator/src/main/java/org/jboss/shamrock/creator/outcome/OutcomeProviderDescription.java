/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package io.quarkus.creator.outcome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class OutcomeProviderDescription<C> {

    static final int PROCESSING = 0b00001;
    static final int PROCESSED  = 0b00010;

    protected final int id;
    protected final OutcomeProvider<C> provider;
    protected List<Class<?>> providedTypes = Collections.emptyList();
    private int flags;

    protected OutcomeProviderDescription(int id, OutcomeProvider<C> provider) {
        this.id = id;
        this.provider = provider;
    }

    protected void addProvidedType(Class<?> providedType) {
        if(providedTypes.isEmpty()) {
            providedTypes = new ArrayList<>(1);
        }
        providedTypes.add(providedType);
    }

    boolean isFlagOn(int flag) {
        return (flags & flag) > 0;
    }

    boolean setFlag(int flag) {
        if((flags & flag) > 0) {
            return false;
        }
        flags ^= flag;
        return true;
    }

    void clearFlag(int flag) {
        if((flags & flag) > 0) {
            flags ^= flag;
        }
    }
}
