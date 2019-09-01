package io.quarkus.deployment.proxy;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import io.quarkus.deployment.util.ClassOutputUtil;
import io.quarkus.gizmo.ClassOutput;

/**
 * A Gizmo {@link ClassOutput} that is able to write the inject the bytecode directly into the classloader
 * The implementation was lifted from jboss-classwriter's org.jboss.classfilewriter.DefaultClassFactory
 *
 * This does NOT work in JDK 12+ where the defineClass has been removed from sun.misc.Unsafe
 */
class InjectIntoClassloaderClassOutput implements ClassOutput {

    private static final Method defineClassMethod = getClassLoaderDefineClassMethod();

    private final ClassLoader classLoader;

    InjectIntoClassloaderClassOutput(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    // get a hold of java.lang.ClassLoader#defineClass
    private static Method getClassLoaderDefineClassMethod() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                public Method run() throws Exception {
                    long overrideOffset;
                    Object unsafe;
                    Class<?> unsafeClass;

                    // we cannot refer to Unsafe in a normal manner because the Java 11 build would fail
                    try {
                        // first we need to grab Unsafe
                        unsafeClass = Class.forName("sun.misc.Unsafe");
                        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                        theUnsafe.setAccessible(true);
                        unsafe = theUnsafe.get(null);
                        Method objectFieldOffsetMethod = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
                        overrideOffset = (long) objectFieldOffsetMethod.invoke(unsafe,
                                AccessibleObject.class.getDeclaredField("override"));

                        // now we gain access to CL.defineClass methods
                        Class<?> cl = ClassLoader.class;
                        Method classLoaderDefineClassMethod = cl.getDeclaredMethod("defineClass", String.class, byte[].class,
                                int.class,
                                int.class);

                        // use Unsafe to crack open both CL.defineClass() methods (instead of using setAccessible())
                        Method putBooleanMethod = unsafeClass.getDeclaredMethod("putBoolean", Object.class, long.class,
                                boolean.class);
                        putBooleanMethod.invoke(unsafe, classLoaderDefineClassMethod, overrideOffset, true);
                        return classLoaderDefineClassMethod;
                    } catch (Exception e) {
                        throw new Error(e);
                    }

                }
            });
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException("Cannot initialize InjectIntoClassloaderClassOutput", pae.getException());
        }
    }

    @Override
    public void write(String name, byte[] data) {
        if (System.getProperty("dumpClass") != null) {
            ClassOutputUtil.dumpClass(name, data);
        }
        try {
            defineClassMethod.invoke(classLoader, name.replace('/', '.'), data, 0, data.length);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
