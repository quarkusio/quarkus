package io.quarkus.bootstrap.runner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MainMethodInvoker {

    // follow the rules outlines in https://openjdk.org/jeps/445 section 'Selecting a main method'
    public static void invoke(Class<?> mainClass, Object args)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        // public static void main(String[] args)
        Method mainWithArgs = null;
        try {
            mainWithArgs = mainClass.getDeclaredMethod("main", String[].class);
            int modifiers = mainWithArgs.getModifiers();
            if (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                if (!Modifier.isPublic(modifiers)) {
                    mainWithArgs.setAccessible(true);
                }
                mainWithArgs.invoke(null, args);
                return;
            }
        } catch (NoSuchMethodException ignored) {

        }

        // public static void main()
        Method mainWithoutArgs = null;
        try {
            mainWithoutArgs = mainClass.getDeclaredMethod("main");
            int modifiers = mainWithoutArgs.getModifiers();
            if (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                if (!Modifier.isPublic(modifiers)) {
                    mainWithoutArgs.setAccessible(true);
                }
                mainWithoutArgs.invoke(null);
                return;
            }
        } catch (NoSuchMethodException ignored) {

        }

        var instance = mainClass.getConstructor().newInstance();

        // public void main(String[] args)
        if (mainWithArgs != null) {
            int modifiers = mainWithArgs.getModifiers();
            if (!Modifier.isPrivate(modifiers)) {
                if (!Modifier.isPublic(modifiers)) {
                    mainWithArgs.setAccessible(true);
                }
                mainWithArgs.invoke(instance, args);
                return;
            }
        }

        // public void main()
        if (mainWithoutArgs != null) {
            int modifiers = mainWithoutArgs.getModifiers();
            if (!Modifier.isPrivate(modifiers)) {
                if (!Modifier.isPublic(modifiers)) {
                    mainWithoutArgs.setAccessible(true);
                }
                mainWithoutArgs.invoke(instance);
                return;
            }
        }

        throw new NoSuchMethodException("Unable to find main method");
    }
}
