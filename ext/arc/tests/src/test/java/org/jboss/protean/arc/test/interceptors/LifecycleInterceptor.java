package org.jboss.protean.arc.test.interceptors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.protean.arc.InvocationContextImpl;

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
