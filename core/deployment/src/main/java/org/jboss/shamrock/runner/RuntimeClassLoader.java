package org.jboss.shamrock.runner;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class RuntimeClassLoader extends ClassLoader implements ClassOutput, Consumer<List<Function<String, Function<ClassVisitor, ClassVisitor>>>> {

    private final Map<String, byte[]> classes = new HashMap<>();

    private volatile List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions = null;

    private final Path applicationClasses;

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
        if (classes.containsKey(name)) {
            return findClass(name);
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
        byte[] bytes = classes.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException();
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public void writeClass(String className, byte[] data) {
        classes.put(className.replace('/', '.'), data);
    }

    @Override
    public void accept(List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions) {
        this.functions = functions;
    }
}
