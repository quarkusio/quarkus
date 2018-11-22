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

package org.jboss.shamrock.faulttolerance.runtime.graal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.smallrye.faulttolerance.DefaultMethodFallbackProvider")
final class Target_io_smallrye_faulttolerance_DefaultMethodFallbackProvider {

    @TargetClass(className = "io.smallrye.faulttolerance.ExecutionContextWithInvocationContext")
    static final class Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext {

    }

    @Substitute
    static Object getFallback(Method fallbackMethod, Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext ctx)
            throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException, Throwable {
        throw new RuntimeException("Not implemented in substrate");
    }
}
