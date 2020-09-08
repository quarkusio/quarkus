package io.quarkus.runner.bootstrap;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.runtime.Quarkus;

public class StartupActionImpl implements StartupAction {

    private static final Logger log = Logger.getLogger(StartupActionImpl.class);

    private final CuratedApplication curatedApplication;
    private final BuildResult buildResult;
    private final QuarkusClassLoader runtimeClassLoader;

    public StartupActionImpl(CuratedApplication curatedApplication, BuildResult buildResult) {
        this.curatedApplication = curatedApplication;
        this.buildResult = buildResult;
        Set<String> eagerClasses = new HashSet<>();
        Map<String, byte[]> transformedClasses = extractTransformers(eagerClasses);
        QuarkusClassLoader baseClassLoader = curatedApplication.getBaseRuntimeClassLoader();
        QuarkusClassLoader runtimeClassLoader;

        //so we have some differences between dev and test mode here.
        //test mode only has a single class loader, while dev uses a disposable runtime class loader
        //that is discarded between restarts
        Map<String, byte[]> resources = new HashMap<>();
        resources.putAll(extractGeneratedResources(true));
        if (curatedApplication.getQuarkusBootstrap().getMode() == QuarkusBootstrap.Mode.DEV) {
            baseClassLoader.reset(extractGeneratedResources(false),
                    transformedClasses);
            runtimeClassLoader = curatedApplication.createRuntimeClassLoader(baseClassLoader,
                    resources, transformedClasses);
        } else {
            resources.putAll(extractGeneratedResources(false));
            baseClassLoader.reset(resources, transformedClasses);
            runtimeClassLoader = baseClassLoader;
        }
        this.runtimeClassLoader = runtimeClassLoader;
    }

    private void handleEagerClasses(QuarkusClassLoader runtimeClassLoader, Set<String> eagerClasses) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors == 1) {
            return;
        }
        //leave one processor for the main startup thread
        ExecutorService loadingExecutor = Executors.newFixedThreadPool(availableProcessors - 1);
        for (String i : eagerClasses) {
            loadingExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        //no need to restore the old TCCL, this thread is going away
                        Thread.currentThread().setContextClassLoader(runtimeClassLoader);
                        runtimeClassLoader.loadClass(i);
                    } catch (ClassNotFoundException e) {
                        log.debug("Failed to eagerly load class", e);
                        //we just ignore this for now, the problem
                        //will be reported for real in the startup sequence
                    }
                }
            });
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //when all the jobs are done we shut down
                //we do this in a new thread to allow the main thread to continue doing startup
                loadingExecutor.shutdown();
            }
        });
        t.start();
    }

    /**
     * Runs the application by running the main method of the main class. As this is a blocking method a new
     * thread is created to run this task.
     *
     * Before this method is called an appropriate exit handler will likely need to
     * be set in {@link io.quarkus.runtime.ApplicationLifecycleManager#setDefaultExitCodeHandler(Consumer)}
     * of the JVM will exit when the app stops.
     */
    public RunningQuarkusApplication runMainClass(String... args) throws Exception {

        //first we hack around class loading in the fork join pool
        ForkJoinClassLoading.setForkJoinClassLoader(runtimeClassLoader);

        //this clears any old state, and gets ready to start again
        ApplicationStateNotification.reset();
        //we have our class loaders
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(runtimeClassLoader);
        final String className = buildResult.consume(MainClassBuildItem.class).getClassName();
        try {
            // force init here
            Class<?> appClass = Class.forName(className, true, runtimeClassLoader);
            Method start = appClass.getMethod("main", String[].class);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setContextClassLoader(runtimeClassLoader);
                    try {
                        start.invoke(null, (Object) (args == null ? new String[0] : args));
                    } catch (Throwable e) {
                        log.error("Error running Quarkus", e);
                        //this can happen if we did not make it to application init
                        if (ApplicationStateNotification.getState() == ApplicationStateNotification.State.INITIAL) {
                            ApplicationStateNotification.notifyStartupFailed(e);
                        }
                    }
                }
            }, "Quarkus Main Thread");
            t.start();
            ApplicationStateNotification.waitForApplicationStart();
            return new RunningQuarkusApplicationImpl(new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        runtimeClassLoader.loadClass(Quarkus.class.getName()).getMethod("blockingExit").invoke(null);
                    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException
                            | ClassNotFoundException e) {
                        log.error("Failed to stop Quarkus", e);
                    } finally {
                        ForkJoinClassLoading.setForkJoinClassLoader(ClassLoader.getSystemClassLoader());
                        if (curatedApplication.getQuarkusBootstrap().getMode() == QuarkusBootstrap.Mode.TEST) {
                            //for tests we just always shut down the curated application, as it is only used once
                            //dev mode might be about to restart, so we leave it
                            curatedApplication.close();
                        }
                    }
                }
            }, runtimeClassLoader);
        } catch (Throwable t) {
            // todo: dev mode expects run time config to be available immediately even if static init didn't complete.
            try {
                final Class<?> configClass = Class.forName(RunTimeConfigurationGenerator.CONFIG_CLASS_NAME, true,
                        runtimeClassLoader);
                configClass.getDeclaredMethod(RunTimeConfigurationGenerator.C_CREATE_BOOTSTRAP_CONFIG.getName())
                        .invoke(null);
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

    }

    /**
     * Runs the application, and returns a handle that can be used to shut it down.
     */
    public RunningQuarkusApplication run(String... args) throws Exception {
        //first
        ForkJoinClassLoading.setForkJoinClassLoader(runtimeClassLoader);

        //we have our class loaders
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(runtimeClassLoader);
            final String className = buildResult.consume(ApplicationClassNameBuildItem.class).getClassName();
            Class<?> appClass;
            try {
                // force init here
                appClass = Class.forName(className, true, runtimeClassLoader);
            } catch (Throwable t) {
                // todo: dev mode expects run time config to be available immediately even if static init didn't complete.
                try {
                    final Class<?> configClass = Class.forName(RunTimeConfigurationGenerator.CONFIG_CLASS_NAME, true,
                            runtimeClassLoader);
                    configClass.getDeclaredMethod(RunTimeConfigurationGenerator.C_CREATE_BOOTSTRAP_CONFIG.getName())
                            .invoke(null);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }

            Method start = appClass.getMethod("start", String[].class);
            Object application = appClass.newInstance();
            start.invoke(application, (Object) args);
            Closeable closeTask = (Closeable) application;
            return new RunningQuarkusApplicationImpl(new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        ClassLoader original = Thread.currentThread().getContextClassLoader();
                        try {
                            // some actions during close can still require the runtime classloader
                            // (e.g. ServiceLoader calls)
                            Thread.currentThread().setContextClassLoader(runtimeClassLoader);
                            closeTask.close();
                        } finally {
                            Thread.currentThread().setContextClassLoader(original);
                            runtimeClassLoader.close();
                        }
                    } finally {
                        ForkJoinClassLoading.setForkJoinClassLoader(ClassLoader.getSystemClassLoader());
                        if (curatedApplication.getQuarkusBootstrap().getMode() == QuarkusBootstrap.Mode.TEST) {
                            //for tests we just always shut down the curated application, as it is only used once
                            //dev mode might be about to restart, so we leave it
                            curatedApplication.close();
                        }
                    }
                }
            }, runtimeClassLoader);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw new RuntimeException("Failed to start Quarkus", e.getCause());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

    }

    @Override
    public ClassLoader getClassLoader() {
        return runtimeClassLoader;
    }

    private Map<String, byte[]> extractTransformers(Set<String> eagerClasses) {
        Map<String, byte[]> ret = new HashMap<>();
        TransformedClassesBuildItem transformers = buildResult.consume(TransformedClassesBuildItem.class);
        for (Set<TransformedClassesBuildItem.TransformedClass> i : transformers.getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass clazz : i) {
                ret.put(clazz.getFileName(), clazz.getData());
                if (clazz.isEager()) {
                    eagerClasses.add(clazz.getClassName());
                }
            }
        }
        return ret;
    }

    private Map<String, byte[]> extractGeneratedResources(boolean applicationClasses) {
        Map<String, byte[]> data = new HashMap<>();
        for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
            if (i.isApplicationClass() == applicationClasses) {
                data.put(i.getName().replace(".", "/") + ".class", i.getClassData());
                if (BootstrapDebug.DEBUG_CLASSES_DIR != null) {
                    try {
                        File debugPath = new File(BootstrapDebug.DEBUG_CLASSES_DIR);
                        if (!debugPath.exists()) {
                            debugPath.mkdir();
                        }
                        File classFile = new File(debugPath, i.getName() + ".class");
                        classFile.getParentFile().mkdirs();
                        try (FileOutputStream classWriter = new FileOutputStream(classFile)) {
                            classWriter.write(i.getClassData());
                        }
                        log.infof("Wrote %s", classFile.getAbsolutePath());
                    } catch (Exception t) {
                        log.errorf(t, "Failed to write debug class files %s", i.getName());
                    }
                }

                String debugSourcesDir = BootstrapDebug.DEBUG_SOURCES_DIR;
                if (debugSourcesDir != null) {
                    try {
                        if (i.getSource() != null) {
                            File debugPath = new File(debugSourcesDir);
                            if (!debugPath.exists()) {
                                debugPath.mkdir();
                            }
                            File sourceFile = new File(debugPath, i.getName() + ".zig");
                            sourceFile.getParentFile().mkdirs();
                            Files.write(sourceFile.toPath(), i.getSource().getBytes(StandardCharsets.UTF_8),
                                    StandardOpenOption.CREATE);
                            log.infof("Wrote source %s", sourceFile.getAbsolutePath());
                        } else {
                            log.infof("Source not available: %s", i.getName());
                        }
                    } catch (Exception t) {
                        log.errorf(t, "Failed to write debug source file %s", i.getName());
                    }
                }
            }
        }
        if (applicationClasses) {
            for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
                data.put(i.getName(), i.getClassData());
            }
        }
        return data;
    }

}
