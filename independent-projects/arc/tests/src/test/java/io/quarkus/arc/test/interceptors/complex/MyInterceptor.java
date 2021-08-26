package io.quarkus.arc.test.interceptors.complex;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@MyBinding
public class MyInterceptor {

    public static AtomicBoolean preDestroyInvoked = new AtomicBoolean(false);
    public static AtomicBoolean postConstructInvoked = new AtomicBoolean(false);
    public static AtomicBoolean aroundConstructInvoked = new AtomicBoolean(false);
    public static AtomicBoolean aroundInvokeInvoked = new AtomicBoolean(false);

    @PreDestroy
    public void preDestroy(InvocationContext ic) {
        preDestroyInvoked.set(true);
    }

    @PostConstruct
    public void postConstruct(InvocationContext ic) {
        postConstructInvoked.set(true);
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
