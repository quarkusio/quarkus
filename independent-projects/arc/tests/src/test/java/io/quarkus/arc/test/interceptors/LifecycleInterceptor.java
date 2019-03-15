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

package io.quarkus.arc.test.interceptors;

import io.quarkus.arc.InvocationContextImpl;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Lifecycle
@Priority(1)
@Interceptor
public class LifecycleInterceptor {

    static final List<Object> AROUND_CONSTRUCTS = new CopyOnWriteArrayList<>();
    static final List<Object> POST_CONSTRUCTS = new CopyOnWriteArrayList<>();
    static final List<Object> PRE_DESTROYS = new CopyOnWriteArrayList<>();

    @PostConstruct
    void simpleInit(InvocationContext ctx) {
        Object bindings = ctx.getContextData().get(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        POST_CONSTRUCTS.add(ctx.getTarget());
    }

    @PreDestroy
    void simpleDestroy(InvocationContext ctx) {
        Object bindings = ctx.getContextData().get(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        PRE_DESTROYS.add(ctx.getTarget());
    }

    @AroundConstruct
    void simpleAroundConstruct(InvocationContext ctx) throws Exception {
        Object bindings = ctx.getContextData().get(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        try {
            AROUND_CONSTRUCTS.add(ctx.getConstructor());
            ctx.proceed();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
