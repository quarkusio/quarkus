package io.quarkus.test.component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    static void registerCreate(String key, Function<SyntheticCreationalContext<?>, InterceptFunction> create) {
        createFunctions.put(key, create);
    }

    static void clear() {
        createFunctions.clear();
    }

}
