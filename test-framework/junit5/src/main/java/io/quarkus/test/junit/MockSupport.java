package io.quarkus.test.junit;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

class MockSupport {

    private static final Deque<List<Object>> contexts = new ArrayDeque<>();

    @SuppressWarnings("unused")
    static void pushContext() {
        contexts.push(new ArrayList<>());
    }

    @SuppressWarnings("unused")
    static void popContext() {
        List<Object> val = contexts.pop();
        for (Object i : val) {
            try {
                i.getClass().getDeclaredMethod("quarkus$$clearMock").invoke(i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static <T> void installMock(T instance, T mock) {
        //due to class loading issues we can't access the interface directly
        List<Object> inst = contexts.peek();
        if (inst == null) {
            throw new IllegalStateException("No test in progress");
        }
        try {
            Method setMethod = instance.getClass().getDeclaredMethod("quarkus$$setMock", Object.class);
            setMethod.invoke(instance, mock);
            inst.add(instance);

        } catch (Exception e) {
            throw new RuntimeException(instance
                    + " is not a normal scoped CDI bean, make sure the bean is a normal scope like @ApplicationScoped or @RequestScoped");

        }
    }
}
