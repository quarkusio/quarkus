package io.quarkus.qute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Martin Kouba
 */
class MethodWrapper implements MemberWrapper {

    private final Method method;

    MethodWrapper(Method method) {
        super();
        this.method = method;
    }

    @Override
    public Object getValue(Object instance) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return method.invoke(instance);
    }

}
