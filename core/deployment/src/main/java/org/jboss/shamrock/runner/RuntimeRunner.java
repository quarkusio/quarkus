package org.jboss.shamrock.runner;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import org.jboss.builder.BuildResult;
import org.jboss.shamrock.deployment.ShamrockAugumentor;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.MainClassBuildItem;
import org.objectweb.asm.ClassVisitor;

/**
 * Class that can be used to run shamrock directly, ececuting the build and runtime
 * steps in the same JVM
 */
public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final RuntimeClassLoader loader;
    private Closeable closeTask;
    private final List<Path> additionalArchives;

    public RuntimeRunner(ClassLoader classLoader, Path target, Path frameworkClassesPath, Path transformerCache, List<Path> additionalArchives) {
        this.target = target;
        this.additionalArchives = additionalArchives;
        this.loader = new RuntimeClassLoader(classLoader, target, frameworkClassesPath, transformerCache);
    }

    @Override
    public void close() throws IOException {
        if (closeTask != null) {
            closeTask.close();
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setContextClassLoader(loader);
        try {
            ShamrockAugumentor.Builder builder = ShamrockAugumentor.builder();
            builder.setRoot(target);
            builder.setClassLoader(loader);
            builder.setOutput(loader);
            for (Path i : additionalArchives) {
                builder.addAdditionalApplicationArchive(i);
            }
            builder.addFinal(BytecodeTransformerBuildItem.class)
                    .addFinal(MainClassBuildItem.class);

            BuildResult result = builder.build().run();
            List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result.consumeMulti(BytecodeTransformerBuildItem.class);
            if (!bytecodeTransformerBuildItems.isEmpty()) {
                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions = new HashMap<>();
                for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                    functions.computeIfAbsent(i.getClassToTransform(), (f) -> new ArrayList<>()).add(i.getVisitorFunction());
                }

                loader.setTransformers(functions);
                if (!functions.isEmpty()) {
                    //transformation can be slow, and classes that are transformed are generally always loaded on startup
                    //to speed this along we eagerly load the classes in parallel
                    //TODO: do we need this? apparently there have been big perf fixes
                    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                    for (Map.Entry<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> entry : functions.entrySet()) {
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    loader.loadClass(entry.getKey(), true);
                                } catch (ClassNotFoundException e) {
                                    //ignore
                                    //this will show up at runtime anyway
                                }
                            }
                        });
                    }
                    executorService.shutdown();

                }
            }


            Class<?> mainClass = loader.findClass(result.consume(MainClassBuildItem.class).getClassName());
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
}
