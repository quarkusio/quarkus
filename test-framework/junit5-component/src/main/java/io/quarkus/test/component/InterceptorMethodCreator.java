package io.quarkus.test.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.quarkus.arc.InterceptorCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class InterceptorMethodCreator implements InterceptorCreator {

    static final String CREATE_KEY = "createKey";

    private static final AtomicInteger idGenerator = new AtomicInteger();

    // filled in the original CL, used to register interceptor methods in the extra CL
    private static final Map<String, String[]> interceptorMethods = new HashMap<>();

    // filled in the extra CL, used to actually invoke interceptor methods
    private static final Map<String, Function<SyntheticCreationalContext<?>, InterceptFunction>> createFunctions = new HashMap<>();

    @Override
    public InterceptFunction create(SyntheticCreationalContext<Object> context) {
        Object createKey = context.getParams().get(CREATE_KEY);
        if (createKey != null) {
            Function<SyntheticCreationalContext<?>, InterceptFunction> createFun = createFunctions.get(createKey);
            if (createFun != null) {
                return createFun.apply(context);
            }
        }
        throw new IllegalStateException("Create function not found: " + createKey);
    }

    // called in the original CL, fills `interceptorMethods`
    static String preregister(Class<?> testClass, Method interceptorMethod) {
        String key = "io_quarkus_test_component_InterceptorMethodCreator_" + idGenerator.incrementAndGet();
        String[] descriptor = new String[3 + interceptorMethod.getParameterCount()];
        descriptor[0] = testClass.getName();
        descriptor[1] = interceptorMethod.getDeclaringClass().getName();
        descriptor[2] = interceptorMethod.getName();
        for (int i = 0; i < interceptorMethod.getParameterCount(); i++) {
            descriptor[3 + i] = interceptorMethod.getParameterTypes()[i].getName();
        }
        interceptorMethods.put(key, descriptor);
        return key;
    }

    static Map<String, String[]> preregistered() {
        return interceptorMethods;
    }

    // called in the extra CL, fills `createFunctions`
    static void register(Map<String, String[]> methods, Deque<?> testInstanceStack) throws ReflectiveOperationException {
        for (Map.Entry<String, String[]> entry : methods.entrySet()) {
            String key = entry.getKey();
            String[] descriptor = entry.getValue();
            Class<?> testClass = Class.forName(descriptor[0]);
            Class<?> declaringClass = Class.forName(descriptor[1]);
            String methodName = descriptor[2];
            int params = descriptor.length - 3;
            Class<?>[] parameterTypes = new Class<?>[params];
            for (int i = 0; i < params; i++) {
                parameterTypes[i] = Class.forName(descriptor[3 + i]);
            }
            Method method = declaringClass.getDeclaredMethod(methodName, parameterTypes);
            boolean isStatic = Modifier.isStatic(method.getModifiers());

            Function<SyntheticCreationalContext<?>, InterceptFunction> fun = ctx -> {
                return ic -> {
                    Object instance = null;
                    if (!isStatic) {
                        for (Object testInstanceData : testInstanceStack) {
                            // the objects on the stack are instances of `TestInstance` in the original CL,
                            // need to obtain the test instance (which in turn comes from the extra CL) reflectively
                            Field field = testInstanceData.getClass().getDeclaredField("testInstance");
                            field.setAccessible(true);
                            Object testInstance = field.get(testInstanceData);
                            if (testInstance.getClass().equals(testClass)) {
                                instance = testInstance;
                                break;
                            }
                        }
                        if (instance == null) {
                            throw new IllegalStateException("Test instance not available");
                        }
                    }
                    if (!method.canAccess(instance)) {
                        method.setAccessible(true);
                    }
                    return method.invoke(instance, ic);
                };
            };

            createFunctions.put(key, fun);
        }
    }

    static void clear() {
        interceptorMethods.clear();
        createFunctions.clear();
        idGenerator.set(0);
    }

}
