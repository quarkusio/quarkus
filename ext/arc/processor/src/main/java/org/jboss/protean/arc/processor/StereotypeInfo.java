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

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

public class StereotypeInfo {

    private final ScopeInfo defaultScope;

    private final List<AnnotationInstance> interceptorBindings;

    private final boolean isAlternative;
    
    private final boolean isNamed;

    private final ClassInfo target;

    public StereotypeInfo(ScopeInfo defaultScope, List<AnnotationInstance> interceptorBindings, boolean isAlternative, boolean isNamed, ClassInfo target) {
        this.defaultScope = defaultScope;
        this.interceptorBindings = interceptorBindings;
        this.isAlternative = isAlternative;
        this.isNamed = isNamed;
        this.target = target;
    }

    public ScopeInfo getDefaultScope() {
        return defaultScope;
    }

    public List<AnnotationInstance> getInterceptorBindings() {
        return interceptorBindings;
    }

    public boolean isAlternative() {
        return isAlternative;
    }
    
    public boolean isNamed() {
        return isNamed;
    }

    public ClassInfo getTarget() {
        return target;
    }

}
