package io.quarkus.deployment.proxy;

import java.lang.invoke.MethodHandles;
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
 *
 * The implementation for JDK 8-11 was lifted from jboss-classwriter's org.jboss.classfilewriter.DefaultClassFactory
 *
 * This implementation for JDK 12+ is based on what is discussed at:
 * http://mail.openjdk.java.net/pipermail/jigsaw-dev/2018-April/013724.html
 * and currently does not work properly - If the generated proxy references classes outside of its package
 * a NoClassDefFoundError is thrown
 */
class InjectIntoClassloaderClassOutput implements ClassOutput {

    private static Method classLoaderDefineClassMethod;
    private static Method privateLookupInMethod;
    private static Method lookupDefineClass;

    static {
        classLoaderDefineClassMethod = getClassLoaderDefineClassMethod();
        if (classLoaderDefineClassMethod == null) {
            // this is the case of JDK 12+
            try {
                privateLookupInMethod = getPrivateLookupInMethod();
                lookupDefineClass = getLookupDefineClassMethod();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Unable to initialize InjectIntoClassloaderClassOutput. Are you running an unsupported JDK version?");
            }
        }
    }

    private final ClassLoader classLoader;
    private final Class<?> anchorClass;

    InjectIntoClassloaderClassOutput(ClassLoader classLoader, Class<?> anchorClass) {
        this.classLoader = classLoader;
        this.anchorClass = anchorClass;
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
                    } catch (NoSuchFieldException e) {
                        if (e.getMessage().contains("override")) { //this we can handle
                            return null;
                        }
                        throw new Exception(e);
                    } catch (Exception e) {
                        throw new Error(e);
                    }

                }
            });
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException("Cannot initialize InjectIntoClassloaderClassOutput", pae.getException());
        }
    }

    private static Method getPrivateLookupInMethod() throws NoSuchMethodException {
        return MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
    }

    private static Method getLookupDefineClassMethod() throws NoSuchMethodException {
        return MethodHandles.Lookup.class.getDeclaredMethod("defineClass", byte[].class);
    }

    @Override
    public void write(String name, byte[] data) {
        if (System.getProperty("dumpClass") != null) {
            ClassOutputUtil.dumpClass(name, data);
        }
        if (classLoaderDefineClassMethod != null) { // normal JDK 8-11 case
            try {
                classLoaderDefineClassMethod.invoke(classLoader, name.replace('/', '.'), data, 0, data.length);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else { // JDK 12+ case
            // TODO: figure out how to solve NoClassDefFoundError for classes outside the package
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                MethodHandles.Lookup privateLookupIn = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, anchorClass,
                        lookup);
                lookupDefineClass.invoke(privateLookupIn, data);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
