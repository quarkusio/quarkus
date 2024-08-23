package io.quarkus.test.junit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.quarkus.runtime.MockedThroughWrapper;

class MockSupport {

    private static final Deque<List<Object>> contexts = new ArrayDeque<>();

    static void pushContext() {
        contexts.push(new ArrayList<>());
    }

    static void popContext() {
        if (contexts.isEmpty()) {
            return; // can happen on error in QuarkusTestResourceLifecycleManagers etc.
        }
        List<Object> val = contexts.pop();
        for (Object i : val) {
            try {
                if (i instanceof MockedThroughWrapper) {
                    ((MockedThroughWrapper) i).clearMock();
                } else {
                    i.getClass().getDeclaredMethod("arc$clearMock").invoke(i);

                    // Enable all observers declared on the mocked bean
                    mockObservers(i, false);
                }
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
            if (instance instanceof MockedThroughWrapper) {
                ((MockedThroughWrapper) instance).setMock(mock);
                inst.add(instance);
            } else {

                Method setMethod = instance.getClass().getDeclaredMethod("arc$setMock", Object.class);
                setMethod.invoke(instance, mock);
                inst.add(instance);

                // Disable all observers declared on the mocked bean
                mockObservers(instance, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(instance
                    + " is not a normal scoped CDI bean, make sure the bean is a normal scope like @ApplicationScoped or @RequestScoped",
                    e);

        }
    }

    private static <T> void mockObservers(T instance, boolean mock) throws NoSuchMethodException, SecurityException,
            ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // io.quarkus.arc.ClientProxy.arc_bean()
        Method getBeanMethod = instance.getClass().getDeclaredMethod("arc_bean");
        Object bean = getBeanMethod.invoke(instance);
        // io.quarkus.arc.InjectableBean.getIdentifier()
        Method getIdMethod = bean.getClass().getDeclaredMethod("getIdentifier");
        String id = getIdMethod.invoke(bean).toString();
        // io.quarkus.arc.impl.ArcContainerImpl.mockObservers(String, boolean)
        Method mockObserversMethod = instance.getClass().getClassLoader().loadClass("io.quarkus.arc.impl.ArcContainerImpl")
                .getDeclaredMethod("mockObservers", String.class, boolean.class);
        mockObserversMethod.invoke(null, id, mock);
    }
}
