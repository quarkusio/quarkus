package org.jboss.shamrock.runner;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.shamrock.deployment.BuildTimeGenerator;
import org.jboss.shamrock.deployment.index.ClassPathArtifactResolver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * Class that can be used to run shamrock directly, ececuting the build and runtime
 * steps in the same JVM
 */
public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final RuntimeClassLoader loader;
    private final TransformerClassLoader transformerClassLoader;
    private Closeable closeTask;

    public RuntimeRunner(Path target) {
        this.target = target;
        this.transformerClassLoader = new TransformerClassLoader(getClass().getClassLoader());
        this.loader = new RuntimeClassLoader(transformerClassLoader);
    }

    public RuntimeRunner(Path target, ClassLoader cl) {
        this.target = target;
        this.transformerClassLoader = new TransformerClassLoader(cl);
        this.loader = new RuntimeClassLoader(transformerClassLoader);
    }

    @Override
    public void close() throws IOException {
        if (closeTask != null) {
            closeTask.close();
        }
    }

    @Override
    public void run() {
        try {
            BuildTimeGenerator buildTimeGenerator = new BuildTimeGenerator(loader, loader, false);
            buildTimeGenerator.run(target);
            transformerClassLoader.accept(buildTimeGenerator.getBytecodeTransformers());
            Class<?> mainClass = loader.findClass(BuildTimeGenerator.MAIN_CLASS);
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);

                Method run = mainClass.getDeclaredMethod("main", String[].class);
                run.invoke(null, (Object) null);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

            Method close = mainClass.getDeclaredMethod("close");

            closeTask = new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        close.invoke(null);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    static class TransformerClassLoader extends ClassLoader implements Consumer<List<Function<String, Function<ClassVisitor, ClassVisitor>>>> {

        private volatile List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions = null;

        public TransformerClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (functions == null) {
                return super.loadClass(name, resolve);
            }
            List<Function<ClassVisitor, ClassVisitor>> transformers = new ArrayList<>();
            for (Function<String, Function<ClassVisitor, ClassVisitor>> function : this.functions) {
                Function<ClassVisitor, ClassVisitor> res = function.apply(name);
                if (res != null) {
                    transformers.add(res);
                }
            }
            if (transformers.isEmpty()) {
                return super.loadClass(name, resolve);
            }
            String fileName = name.replace(".", "/") + ".class";
            try {

                try (InputStream in = getResourceAsStream(fileName)) {
                    ClassReader cr = new ClassReader(in);
                    ClassWriter writer = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    ClassVisitor visitor = writer;
                    for (Function<ClassVisitor, ClassVisitor> i : transformers) {
                        visitor = i.apply(visitor);
                    }
                    cr.accept(visitor, 0);
                    byte[] data = writer.toByteArray();
                    return defineClass(name, data, 0, data.length);
                }
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to read class file", e);
            }
        }


        @Override
        public void accept(List<Function<String, Function<ClassVisitor, ClassVisitor>>> functions) {
            this.functions = functions;
        }
    }
}
