package io.quarkus.deployment.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class FirstArgInvocationHandler implements InvocationHandler {

    public int invocationCount = 0;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        invocationCount++;
        if ((args != null) && args.length >= 1) {
            return args[0];
        }
        return null;
    }
}
