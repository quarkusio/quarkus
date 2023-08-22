package io.quarkus.test.component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.mockito.Mockito;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class MockBeanCreator implements BeanCreator<Object> {
    private static final Logger LOG = Logger.getLogger(MockBeanCreator.class);

    static final String CREATE_KEY = "createKey";

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private static final Map<String, Function<SyntheticCreationalContext<?>, ?>> createFunctions = new HashMap<>();

    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        Object createKey = context.getParams().get(CREATE_KEY);
        if (createKey != null) {
            Function<SyntheticCreationalContext<?>, ?> createFun = createFunctions.get(createKey);
            if (createFun != null) {
                return createFun.apply(context);
            } else {
                throw new IllegalStateException("Create function not found: " + createKey);
            }
        }
        Class<?> implementationClass = (Class<?>) context.getParams().get("implementationClass");
        LOG.debugf("Mock created for: %s", implementationClass);
        return Mockito.mock(implementationClass);
    }

    static String registerCreate(Function<SyntheticCreationalContext<?>, ?> create) {
        String key = "io_quarkus_test_component_MockBeanCreator_" + idGenerator.incrementAndGet();
        createFunctions.put(key, create);
        return key;
    }

    static void clear() {
        createFunctions.clear();
        idGenerator.set(0);
    }

}
