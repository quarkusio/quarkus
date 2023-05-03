package io.quarkus.funqy.runtime;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FunctionRegistry {
    protected Map<String, FunctionInvoker> functions = new HashMap<>();

    public void register(Class clz, String methodName, String descriptor, String functionName) {
        for (Method m : clz.getMethods()) {
            if (m.getName().equals(methodName) && descriptor.equals(getMethodDescriptor(m))) {
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

    private static String getDescriptorForClass(final Class c) {
        if (c.isPrimitive()) {
            if (c == byte.class)
                return "B";
            if (c == char.class)
                return "C";
            if (c == double.class)
                return "D";
            if (c == float.class)
                return "F";
            if (c == int.class)
                return "I";
            if (c == long.class)
                return "J";
            if (c == short.class)
                return "S";
            if (c == boolean.class)
                return "Z";
            if (c == void.class)
                return "V";
            throw new RuntimeException("Unrecognized primitive " + c);
        }
        if (c.isArray())
            return c.getName().replace('.', '/');
        return ('L' + c.getName() + ';').replace('.', '/');
    }

    private static String getMethodDescriptor(Method m) {
        String s = "(";
        for (final Class c : m.getParameterTypes())
            s += getDescriptorForClass(c);
        s += ')';
        return s + getDescriptorForClass(m.getReturnType());
    }
}
