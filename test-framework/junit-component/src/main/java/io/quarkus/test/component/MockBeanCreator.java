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

    static final String CREATE_KEY = "createKey";

    private static final Logger LOG = Logger.getLogger(MockBeanCreator.class);

    private static final Map<String, Function<SyntheticCreationalContext<?>, ?>> createFunctions = new HashMap<>();

    // test class -> id generator
    private static final Map<String, AtomicInteger> idGenerators = new HashMap<>();

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

    static String registerCreate(String testClass, Function<SyntheticCreationalContext<?>, ?> create) {
        AtomicInteger id = idGenerators.computeIfAbsent(testClass, k -> new AtomicInteger());
        String key = testClass + id.incrementAndGet();
        // we rely on deterministic registration which means that mock configurators are processed in the same order during build and also when a test is executed
        createFunctions.put(key, create);
        return key;
    }

    static void clear() {
        createFunctions.clear();
        idGenerators.clear();
    }

}
