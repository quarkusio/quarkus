package io.quarkus.deployment.dev;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public class IsolatedDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>>, Closeable {

    private static final Logger log = Logger.getLogger(DevModeMain.class);
    public static final String APP_ROOT = "app-root";

    private volatile DevModeContext context;

    private final List<HotReplacementSetup> hotReplacementSetups = new ArrayList<>();
    private static volatile RunningQuarkusApplication runner;
    static volatile Throwable deploymentProblem;
    static volatile RuntimeUpdatesProcessor runtimeUpdatesProcessor;
    private static volatile CuratedApplication curatedApplication;
    private static volatile AugmentAction augmentAction;
    private static volatile boolean restarting;
    private static volatile boolean firstStartCompleted;
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private synchronized void firstStart() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.createInitialRuntimeApplication();
                //this is a bit yuck, but we need replace the default
                //exit handler in the runtime class loader
                //TODO: look at implementing a common core classloader, that removes the need for this sort of crappy hack
                curatedApplication.getBaseRuntimeClassLoader().loadClass(ApplicationLifecycleManager.class.getName())
                        .getMethod("setDefaultExitCodeHandler", Consumer.class)
                        .invoke(null, new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) {
                                if (restarting || ApplicationLifecycleManager.isVmShuttingDown()
                                        || context.isAbortOnFailedStart()) {
                                    return;
                                }
                                System.out.println("Quarkus application exited with code " + integer);
                                System.out.println("Press Enter to restart or Ctrl + C to quit");
                                try {
                                    while (System.in.read() != '\n') {
                                        //noop
                                    }
                                    while (System.in.available() > 0) {
                                        System.in.read();
                                    }
                                    System.out.println("Restarting...");
                                    runtimeUpdatesProcessor.checkForChangedClasses();
                                    restartApp(runtimeUpdatesProcessor.checkForFileChange());
                                } catch (Exception e) {
                                    log.error("Failed to restart", e);
                                }
                            }
                        });
                runner = start.runMainClass(context.getArgs());
                firstStartCompleted = true;
            } catch (Throwable t) {
                deploymentProblem = t;
                if (context.isAbortOnFailedStart()) {
                    log.error("Failed to start quarkus", t);
                } else {
                    //we need to set this here, while we still have the correct TCCL
                    //this is so the config is still valid, and we can read HTTP config from application.properties
                    log.error("Failed to start Quarkus", t);
                    log.info("Attempting to start hot replacement endpoint to recover from previous Quarkus startup failure");
                    if (runtimeUpdatesProcessor != null) {
                        Thread.currentThread().setContextClassLoader(curatedApplication.getBaseRuntimeClassLoader());

                        try {
                            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                                Class<?> cl = Thread.currentThread().getContextClassLoader()
                                        .loadClass(LoggingSetupRecorder.class.getName());
                                cl.getMethod("handleFailedStart").invoke(null);
                            }
                            runtimeUpdatesProcessor.startupFailed();
                        } catch (Exception e) {
                            t.addSuppressed(new RuntimeException("Failed to recover after failed start", e));
                            throw new RuntimeException(t);
                        }
                    }
                }

            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public synchronized void restartApp(Set<String> changedResources) {
        restarting = true;
        stop();
        Timing.restart(curatedApplication.getAugmentClassLoader());
        deploymentProblem = null;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.reloadExistingApplication(firstStartCompleted, changedResources);
                runner = start.runMainClass(context.getArgs());
                firstStartCompleted = true;
            } catch (Throwable t) {
                deploymentProblem = t;
                log.error("Failed to start quarkus", t);
            }
        } finally {
            restarting = false;
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context, Path appRoot)
            throws Exception {
        if (!context.getAllModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getAllModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            ClassLoaderCompiler compiler;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(), curatedApplication,
                        compilationProviders, context);
            } catch (Exception e) {
                log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(appRoot, context, compiler,
                    this::restartApp, null);

            for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class,
                    curatedApplication.getBaseRuntimeClassLoader())) {
                hotReplacementSetups.add(service);
                service.setupHotDeployment(processor);
                processor.addHotReplacementSetup(service);
            }
            return processor;
        }
        return null;
    }

    public void stop() {
        if (runner != null) {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(runner.getClassLoader());
            try {
                runner.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        QuarkusConfigFactory.setConfig(null);

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable ignored) {
            // just means no config was installed, which is fine
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        runner = null;
    }

    public void close() {
        //don't attempt to restart in the exit code handler
        restarting = true;
        try {
            stop();
        } finally {
            try {
                try {
                    runtimeUpdatesProcessor.close();
                } catch (IOException e) {
                    log.error("Failed to close compiler", e);
                }
                for (HotReplacementSetup i : hotReplacementSetups) {
                    i.close();
                }
            } finally {
                curatedApplication.close();
            }
        }
    }

    //the main entry point, but loaded inside the augmentation class loader
    @Override
    public void accept(CuratedApplication o, Map<String, Object> o2) {
        Timing.staticInitStarted(o.getBaseRuntimeClassLoader());
        //https://github.com/quarkusio/quarkus/issues/9748
        //if you have an app with all daemon threads then the app thread
        //may be the only thread keeping the JVM alive
        //during the restart process when this thread is stopped then
        //the JVM will die
        //we start this thread to keep the JVM alive until the shutdown hook is run
        //even for command mode we still want the JVM to live until it receives
        //a signal to make the 'press enter to restart' function to work
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    shutdownLatch.await();
                } catch (InterruptedException ignore) {

                }
            }
        }, "Quarkus Devmode keep alive thread").start();
        try {
            curatedApplication = o;

            Object potentialContext = o2.get(DevModeContext.class.getName());
            if (potentialContext instanceof DevModeContext) {
                context = (DevModeContext) potentialContext;
            } else {
                //this was from the external class loader
                //we need to copy it into this one
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream oo = new ObjectOutputStream(out);
                oo.writeObject(potentialContext);
                context = (DevModeContext) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
            }

            augmentAction = new AugmentActionImpl(curatedApplication,
                    Collections.singletonList(new Consumer<BuildChainBuilder>() {
                        @Override
                        public void accept(BuildChainBuilder buildChainBuilder) {
                            buildChainBuilder.addBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    //we need to make sure all hot reloadable classes are application classes
                                    context.produce(new ApplicationClassPredicateBuildItem(new Predicate<String>() {
                                        @Override
                                        public boolean test(String s) {
                                            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                                                    .getContextClassLoader();
                                            //if the class file is present in this (and not the parent) CL then it is an application class
                                            List<ClassPathElement> res = cl
                                                    .getElementsWithResource(s.replace(".", "/") + ".class", true);
                                            return !res.isEmpty();
                                        }
                                    }));
                                }
                            }).produces(ApplicationClassPredicateBuildItem.class).build();
                        }
                    }));
            runtimeUpdatesProcessor = setupRuntimeCompilation(context, (Path) o2.get(APP_ROOT));
            if (runtimeUpdatesProcessor != null) {
                runtimeUpdatesProcessor.checkForFileChange();
                runtimeUpdatesProcessor.checkForChangedClasses();
            }
            firstStart();

            //        doStart(false, Collections.emptySet());
            if (deploymentProblem != null || runtimeUpdatesProcessor.getCompileProblem() != null) {
                if (context.isAbortOnFailedStart()) {
                    throw new RuntimeException(
                            deploymentProblem == null ? runtimeUpdatesProcessor.getCompileProblem() : deploymentProblem);
                }
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdownLatch.countDown();
                    synchronized (DevModeMain.class) {
                        if (runner != null) {
                            try {
                                close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, "Quarkus Shutdown Thread"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
