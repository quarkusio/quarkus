package org.jboss.resteasy.reactive.server.vertx.test.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.resteasy.reactive.server.processor.util.GeneratedClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ResteasyReactiveTestClassLoader extends URLClassLoader {

    final Map<String, byte[]> generatedClasses;
    final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers;

    public ResteasyReactiveTestClassLoader(URL[] urls, ClassLoader parent, List<GeneratedClass> generatedClasses,
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers) {
        super(urls, parent);
        this.transformers = transformers;
        this.generatedClasses = new HashMap<>();
        for (var i : generatedClasses) {
            this.generatedClasses.put(i.getName(), i.getData());
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        var loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        if (generatedClasses.containsKey(name)) {
            byte[] data = generatedClasses.get(name);
            return defineClass(name, data, 0, data.length);
        }
        if (transformers.containsKey(name)) {
            try (InputStream resource = super.getResourceAsStream(name.replace(".", "/") + ".class")) {
                if (resource != null) {
                    byte[] data;
                    ClassReader cr = new ClassReader(resource.readAllBytes());
                    ClassWriter writer = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    ClassVisitor visitor = writer;
                    for (BiFunction<String, ClassVisitor, ClassVisitor> i : transformers.get(name)) {
                        visitor = i.apply(name, visitor);
                    }
                    cr.accept(visitor, 0);
                    data = writer.toByteArray();
                    return defineClass(name, data, 0, data.length);
                } else {
                    throw new RuntimeException(
                            "Could not find " + name + " to transform, make sure it is added to the test archive");
                }

            } catch (IOException e) {
                return super.findClass(name);
            }
        }
        return super.findClass(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
        }
    }
}
