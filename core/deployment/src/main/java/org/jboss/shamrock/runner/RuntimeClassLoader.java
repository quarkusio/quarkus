package org.jboss.shamrock.runner;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.shamrock.deployment.ClassOutput;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import sun.misc.Unsafe;

public class RuntimeClassLoader extends ClassLoader implements ClassOutput, Consumer<List<Function<String, Function<ClassVisitor, ClassVisitor>>>> {

    private final Map<String, byte[]> appClasses = new HashMap<>();
    private final Map<String, byte[]> frameworkClasses = new HashMap<>();

    private volatile List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions = null;

    private final Path applicationClasses;

    private static java.lang.reflect.Method defineClass1, defineClass2;

    static {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    Class<?> cl = Class.forName("java.lang.ClassLoader");
                    final String name = "defineClass";

                    // get Unsafe singleton instance
                    Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
                    singleoneInstanceField.setAccessible(true);
                    Unsafe theUnsafe = (Unsafe) singleoneInstanceField.get(null);

                    // get the offset of the override field in AccessibleObject
                    long overrideOffset = theUnsafe.objectFieldOffset(AccessibleObject.class.getDeclaredField("override"));

                    defineClass1 = cl.getDeclaredMethod(name, new Class[]{String.class, byte[].class, int.class, int.class});
                    defineClass2 = cl.getDeclaredMethod(name, new Class[]{String.class, byte[].class, int.class, int.class, ProtectionDomain.class});

                    // make both accessible
                    theUnsafe.putBoolean(defineClass1, overrideOffset, true);
                    theUnsafe.putBoolean(defineClass2, overrideOffset, true);
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException("cannot initialize ClassPool", pae.getException());
        }
    }

    public RuntimeClassLoader(ClassLoader parent, Path applicationClasses) {
        super(parent);
        this.applicationClasses = applicationClasses;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> ex = findLoadedClass(name);
        if (ex != null) {
            return ex;
        }
        if (appClasses.containsKey(name)) {
            return findClass(name);
        }
        if(frameworkClasses.containsKey(name)) {
            return toClass(name, frameworkClasses.get(name), getParent());
        }

        String fileName = name.replace(".", "/") + ".class";
        Path classLoc = applicationClasses.resolve(fileName);
        if (Files.exists(classLoc)) {
            byte[] buf = new byte[1024];
            int r;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (FileInputStream in = new FileInputStream(classLoc.toFile())) {
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to load class", e);
            }
            byte[] bytes = out.toByteArray();
            bytes = handleTransform(name, bytes);
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.loadClass(name, resolve);
    }

    private byte[] handleTransform(String name, byte[] bytes) {
        if (functions == null || functions.isEmpty()) {
            return bytes;
        }
        List<Function<ClassVisitor, ClassVisitor>> transformers = new ArrayList<>();
        for (Function<String, Function<ClassVisitor, ClassVisitor>> function : this.functions) {
            Function<ClassVisitor, ClassVisitor> res = function.apply(name);
            if (res != null) {
                transformers.add(res);
            }
        }
        if (transformers.isEmpty()) {
            return bytes;
        }

        ClassReader cr = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = writer;
        for (Function<ClassVisitor, ClassVisitor> i : transformers) {
            visitor = i.apply(visitor);
        }
        cr.accept(visitor, 0);
        byte[] data = writer.toByteArray();
        return data;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = appClasses.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException();
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public void writeClass(boolean applicationClass, String className, byte[] data) {
        if (applicationClass) {
            appClasses.put(className.replace('/', '.'), data);
        } else {
            appClasses.put(className.replace('/', '.'), data);
        }
    }

    @Override
    public void accept(List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions) {
        this.functions = functions;
    }


    static Class<?> toClass(String name, byte[] data, ClassLoader loader) {
        try {
            java.lang.reflect.Method method;
            Object[] args;
            method = defineClass1;
            args = new Object[]{name, data, 0, data.length};
            return toClass2(method, loader, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static synchronized Class<?> toClass2(Method method, ClassLoader loader, Object[] args) throws Exception {
        Class<?> clazz = Class.class.cast(method.invoke(loader, args));
        return clazz;
    }

}
