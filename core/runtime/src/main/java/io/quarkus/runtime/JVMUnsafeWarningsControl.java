package io.quarkus.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class JVMUnsafeWarningsControl {

    /**
     * This is a horrible hack to disable the Unsafe-related warnings that are printed on startup:
     * we know about the problem, we're working on it, and there's no need to print a warning scaring our
     * users with it.
     */
    public static void disableUnsafeRelatedWarnings() {
        //No need for this in native image
        //No need for this in JVMs earlier than 24
        if (ImageMode.current().isNativeImage() || Runtime.version().feature() < 24) {
            return;
        }
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Method trySetMemoryAccessWarnedMethod = unsafeClass.getDeclaredMethod("trySetMemoryAccessWarned");
            trySetMemoryAccessWarnedMethod.setAccessible(true);
            trySetMemoryAccessWarnedMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            //let's ignore it - if we failed with our horrible hack, worst that could happen is that the ugly warning is printed
        }
    }
}
