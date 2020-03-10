package io.quarkus.funqy.runtime;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FunctionRegistry {
    protected Map<String, FunctionInvoker> functions = new HashMap<>();

    public void register(Class clz, String methodName, String functionName) {
        for (Method m : clz.getMethods()) {
            if (m.getName().equals(methodName)) {
                functions.put(functionName, new FunctionInvoker(functionName, clz, m));
            }
        }
    }

    public FunctionInvoker matchInvoker(String name) {
        return functions.get(name);
    }

    public Collection<FunctionInvoker> invokers() {
        return functions.values();
    }
}
