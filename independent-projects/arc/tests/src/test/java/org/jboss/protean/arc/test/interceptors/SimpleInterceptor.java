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

package org.jboss.quarkus.arc.test.interceptors;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.quarkus.arc.InvocationContextImpl;

@Simple
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @Inject
    Counter counter;

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        Object bindings = ctx.getContextData().get(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        return "" + counter.get() + ctx.proceed() + counter.incrementAndGet();
    }
}
