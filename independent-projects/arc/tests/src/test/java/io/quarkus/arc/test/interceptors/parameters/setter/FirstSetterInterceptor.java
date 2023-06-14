package io.quarkus.arc.test.interceptors.parameters.setter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Setter
@Priority(1)
@Interceptor
public class FirstSetterInterceptor {

    @AroundInvoke
    Object fooAroundInvoke(InvocationContext ctx) throws Exception {
        assertEquals("bar", ctx.getParameters()[0]);
        ctx.setParameters(new String[] { "first" });
        Object ret = ctx.proceed();
        // getParameters() should return the value set by the interceptor with higher priority
        assertEquals("second", ctx.getParameters()[0]);
        return ret;
    }
}
