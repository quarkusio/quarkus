package io.quarkus.arc.test.interceptors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ArcInvocationContext;

@Lifecycle
@Priority(1)
@Interceptor
public class LifecycleInterceptor {

    static final List<Object> AROUND_CONSTRUCTS = new CopyOnWriteArrayList<>();
    static final List<Object> POST_CONSTRUCTS = new CopyOnWriteArrayList<>();
    static final List<Object> PRE_DESTROYS = new CopyOnWriteArrayList<>();

    @PostConstruct
    void simpleInit(InvocationContext ctx) throws Exception {
        Object bindings = ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        POST_CONSTRUCTS.add(ctx.getTarget());
        ctx.proceed();
    }

    @PreDestroy
    void simpleDestroy(InvocationContext ctx) throws Exception {
        Object bindings = ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        PRE_DESTROYS.add(ctx.getTarget());
        ctx.proceed();
    }

    @AroundConstruct
    void simpleAroundConstruct(InvocationContext ctx) throws Exception {
        Object bindings = ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
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
