package io.quarkus.spring.web.deployment;

import org.jboss.jandex.DotName;

// TODO when this is going to be needed elsewhere, refactor to move it to a common location
public class TypesUtil {

    private final ClassLoader classLoader;

    public TypesUtil(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public boolean isAssignable(Class<?> clazz, DotName dotName) {
        return clazz.isAssignableFrom(load(dotName.toString()));
    }

    private Class<?> load(String className) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
        }
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
