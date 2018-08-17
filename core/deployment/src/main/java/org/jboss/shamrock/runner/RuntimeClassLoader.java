package org.jboss.shamrock.runner;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.shamrock.deployment.ClassOutput;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import sun.misc.Unsafe;

public class RuntimeClassLoader extends ClassLoader implements ClassOutput, Consumer<List<Function<String, Function<ClassVisitor, ClassVisitor>>>> {

    private final Map<String, byte[]> appClasses = new HashMap<>();
    private final Set<String> frameworkClasses = new HashSet<>();

    private volatile List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions = null;

    private final Path applicationClasses;
    static {
        registerAsParallelCapable();
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
        if(frameworkClasses.contains(name)) {
            return super.loadClass(name, resolve);
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
            //this is pretty horrible
            //basically we add the framework level classes to the file system
            //in the same dir as the actual app classes
            //however as we add them to the frameworkClasses set we know to load them
            //from the parent CL
            frameworkClasses.add(className.replace('/', '.'));
            Path fileName = applicationClasses.resolve(className.replace(".", "/") + ".class");
            try {
                Files.createDirectories(fileName.getParent());
                try(FileOutputStream out = new FileOutputStream(fileName.toFile())) {
                    out.write(data);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void accept(List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions) {
        this.functions = functions;
    }
}
