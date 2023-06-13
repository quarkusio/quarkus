package io.quarkus.arc.test.interceptors.parameters.setter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Setter
@Priority(2)
@Interceptor
public class SecondSetterInterceptor {

    @AroundInvoke
    Object fooAroundInvoke(InvocationContext ctx) throws Exception {
        assertEquals("first", ctx.getParameters()[0]);
        ctx.setParameters(new String[] { "second" });
        assertEquals("second", ctx.getParameters()[0]);
        return ctx.proceed();
    }
}
