package io.quarkus.test.component;

import static io.quarkus.test.component.QuarkusComponentTestExtension.KEY_TEST_INSTANCE;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.arc.InterceptorCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class InterceptorMethodCreator implements InterceptorCreator {

    static final String CREATE_KEY = "createKey";

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

    static String[] descriptor(Method interceptorMethod) {
        String[] descriptor = new String[2 + interceptorMethod.getParameterCount()];
        descriptor[0] = interceptorMethod.getDeclaringClass().getName();
        descriptor[1] = interceptorMethod.getName();
        for (int i = 0; i < interceptorMethod.getParameterCount(); i++) {
            descriptor[2 + i] = interceptorMethod.getParameterTypes()[i].getName();
        }
        return descriptor;
    }

    static void register(ExtensionContext context, Map<String, String[]> interceptorMethods)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        for (Entry<String, String[]> e : interceptorMethods.entrySet()) {
            String key = e.getKey();
            String[] descriptor = e.getValue();
            Class<?> declaringClass = Class.forName(descriptor[0]);
            String methodName = descriptor[1];
            int params = descriptor.length - 2;
            Class<?>[] parameterTypes = new Class<?>[params];
            for (int i = 0; i < params; i++) {
                parameterTypes[i] = Class.forName(descriptor[2 + i]);
            }
            Method method = declaringClass.getDeclaredMethod(methodName, parameterTypes);
            boolean isStatic = Modifier.isStatic(method.getModifiers());

            Function<SyntheticCreationalContext<?>, InterceptFunction> fun = ctx -> {
                return ic -> {
                    Object instance = QuarkusComponentTestExtension.store(context).get(KEY_TEST_INSTANCE);
                    if (!isStatic) {
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
        createFunctions.clear();
    }

}
