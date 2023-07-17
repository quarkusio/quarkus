package io.quarkus.arc.test.interceptors.complex;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@MyBinding
public class MyInterceptor {

    public static AtomicBoolean preDestroyInvoked = new AtomicBoolean(false);
    public static AtomicBoolean postConstructInvoked = new AtomicBoolean(false);
    public static AtomicBoolean aroundConstructInvoked = new AtomicBoolean(false);
    public static AtomicBoolean aroundInvokeInvoked = new AtomicBoolean(false);

    @PreDestroy
    public void preDestroy(InvocationContext ic) throws Exception {
        preDestroyInvoked.set(true);
        ic.proceed();
    }

    @PostConstruct
    public void postConstruct(InvocationContext ic) throws Exception {
        postConstructInvoked.set(true);
        ic.proceed();
    }

    @AroundConstruct
    public void aroundConstruct(InvocationContext ic) throws Exception {
        aroundConstructInvoked.set(true);
        ic.proceed();
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ic) throws Exception {
        aroundInvokeInvoked.set(true);
        return ic.proceed();
    }

    public static void reset() {
        preDestroyInvoked.set(false);
        postConstructInvoked.set(false);
        aroundConstructInvoked.set(false);
        aroundInvokeInvoked.set(false);
    }
}
