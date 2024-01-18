package io.quarkus.deployment.dev;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jboss.logging.Logger;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.ClassChangeInformation;
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
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.deployment.steps.ClassTransformingBuildStep;
import io.quarkus.dev.appstate.ApplicationStartException;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.DeploymentFailedStartHandler;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public class IsolatedDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>>, Closeable {

    private static final Logger log = Logger.getLogger(IsolatedDevModeMain.class);
    public static final String APP_ROOT = "app-root";

    private volatile DevModeContext context;

    private final List<HotReplacementSetup> hotReplacementSetups = new ArrayList<>();
    private static volatile RunningQuarkusApplication runner;
    static volatile Throwable deploymentProblem;
    private static volatile CuratedApplication curatedApplication;
    private static volatile AugmentAction augmentAction;
    private static volatile boolean restarting;
    private static volatile boolean firstStartCompleted;
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private Thread shutdownThread;
    private CodeGenWatcher codeGenWatcher;
    private static volatile ConsoleStateManager.ConsoleContext consoleContext;
    private final List<DevModeListener> listeners = new ArrayList<>();

    private synchronized void firstStart() {

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                //this is a bit yuck, but we need replace the default
                //exit handler in the runtime class loader
                //TODO: look at implementing a common core classloader, that removes the need for this sort of crappy hack
                curatedApplication.getBaseRuntimeClassLoader().loadClass(ApplicationLifecycleManager.class.getName())
                        .getMethod("setDefaultExitCodeHandler", Consumer.class)
                        .invoke(null, new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) {
                                if (restarting || ApplicationLifecycleManager.isVmShuttingDown()
                                        || context.isAbortOnFailedStart() ||
                                        context.isTest()) {
                                    return;
                                }
                                if (consoleContext == null) {
                                    consoleContext = ConsoleStateManager.INSTANCE
                                            .createContext("Completed Application");
                                }
                                //this sucks, but when we get here logging is gone
                                //so we just setup basic console logging
                                InitialConfigurator.DELAYED_HANDLER.addHandler(new ConsoleHandler(
                                        ConsoleHandler.Target.SYSTEM_OUT,
                                        new ColorPatternFormatter("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")));
                                consoleContext.reset(new ConsoleCommand(' ', "Restarts the application", "to restart", 0, null,
                                        () -> {
                                            consoleContext.reset();
                                            RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
                                        }));
                            }
                        });

                StartupAction start = augmentAction.createInitialRuntimeApplication();

                runner = start.runMainClass(context.getArgs());
                RuntimeUpdatesProcessor.INSTANCE.setConfiguredInstrumentationEnabled(
                        runner.getConfigValue("quarkus.live-reload.instrumentation", Boolean.class).orElse(false));
                firstStartCompleted = true;
                notifyListenersAfterStart();

            } catch (Throwable t) {
                Throwable rootCause = t;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                if (!(rootCause instanceof BindException)) {
                    deploymentProblem = t;
                    if (!context.isAbortOnFailedStart()) {
                        //we need to set this here, while we still have the correct TCCL
                        //this is so the config is still valid, and we can read HTTP config from application.properties
                        log.info(
                                "Attempting to start live reload endpoint to recover from previous Quarkus startup failure");

                        // Make sure to change the application state so that QuarkusDevModeTest does not hang when
                        // allowFailedStart=true and the build fails when the dev mode starts initially
                        ApplicationStateNotification.notifyStartupFailed(t);

                        if (RuntimeUpdatesProcessor.INSTANCE != null) {
                            Thread.currentThread().setContextClassLoader(curatedApplication.getBaseRuntimeClassLoader());
                            try {
                                if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                                    Class<?> cl = Thread.currentThread().getContextClassLoader()
                                            .loadClass(LoggingSetupRecorder.class.getName());
                                    cl.getMethod("handleFailedStart").invoke(null);
                                }
                                RuntimeUpdatesProcessor.INSTANCE.startupFailed();
                                //try and wait till logging is setup

                                //this exception has already been logged, so don't log it again
                                if (!(t instanceof ApplicationStartException)) {
                                    log.error("Failed to start quarkus", t);
                                }
                            } catch (Exception e) {
                                close();
                                log.error("Failed to start quarkus", t);
                                log.error("Failed to recover after failed start", e);
                                //this is the end of the road, we just exit
                                //generally we only hit this if something is already listening on the HTTP port
                                //or the system config is so broken we can't start HTTP
                                System.exit(1);
                            }
                        }
                    } else {
                        log.error("Failed to start quarkus", t);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public void restartCallback(Set<String> changedResources, ClassScanResult result) {
        restartApp(changedResources,
                new ClassChangeInformation(result.changedClassNames, result.deletedClassNames, result.addedClassNames));
    }

    public synchronized void restartApp(Set<String> changedResources, ClassChangeInformation classChangeInformation) {
        restarting = true;
        if (consoleContext != null) {
            consoleContext.reset();
        }
        stop();
        Timing.restart(curatedApplication.getAugmentClassLoader());
        deploymentProblem = null;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.reloadExistingApplication(firstStartCompleted, changedResources,
                        classChangeInformation);
                runner = start.runMainClass(context.getArgs());
                if (!firstStartCompleted) {
                    notifyListenersAfterStart();
                    firstStartCompleted = true;
                }
            } catch (Throwable t) {
                deploymentProblem = t;
                Throwable rootCause = t;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                if (!(rootCause instanceof BindException)) {
                    log.error("Failed to start quarkus", t);
                    Thread.currentThread().setContextClassLoader(curatedApplication.getAugmentClassLoader());
                    LoggingSetupRecorder.handleFailedStart();
                }
            }
        } finally {
            restarting = false;
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private void notifyListenersAfterStart() {
        for (DevModeListener listener : ServiceLoader.load(DevModeListener.class)) {
            listeners.add(listener);
        }
        listeners.sort(Comparator.comparingInt(DevModeListener::order));

        for (DevModeListener listener : listeners) {
            try {
                listener.afterFirstStart(runner);
            } catch (Exception e) {
                log.warn("Unable to invoke 'afterFirstStart' of " + listener.getClass(), e);
            }
        }
    }

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context, Path appRoot, DevModeType devModeType)
            throws Exception {
        if (!context.getAllModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getAllModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            QuarkusCompiler compiler = new QuarkusCompiler(curatedApplication, compilationProviders, context);
            TestSupport testSupport = null;
            if (devModeType == DevModeType.LOCAL) {
                testSupport = new TestSupport(curatedApplication, compilationProviders, context, devModeType);
            }

            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(appRoot, context, compiler,
                    devModeType, this::restartCallback, null, new BiFunction<String, byte[], byte[]>() {
                        @Override
                        public byte[] apply(String s, byte[] bytes) {
                            return ClassTransformingBuildStep.transform(s, bytes);
                        }
                    }, testSupport);

            for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class,
                    curatedApplication.getBaseRuntimeClassLoader())) {
                hotReplacementSetups.add(service);
                service.setupHotDeployment(processor);
                processor.addHotReplacementSetup(service);
            }
            for (DeploymentFailedStartHandler service : ServiceLoader.load(DeploymentFailedStartHandler.class,
                    curatedApplication.getAugmentClassLoader())) {
                processor.addDeploymentFailedStartHandler(new Runnable() {
                    @Override
                    public void run() {
                        ClassLoader old = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.currentThread().setContextClassLoader(curatedApplication.getAugmentClassLoader());
                            service.handleFailedInitialStart();
                        } finally {
                            Thread.currentThread().setContextClassLoader(old);
                        }
                    }
                });
            }
            DevConsoleManager.setQuarkusBootstrap(curatedApplication.getQuarkusBootstrap());
            DevConsoleManager.setHotReplacementContext(processor);
            return processor;
        }
        return null;
    }

    public void stop() {
        if (runner != null) {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(runner.getClassLoader());
            try {
                try {
                    runner.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    // TODO is this even useful?
                    //  It's not using the config factory from the right classloader...
                    QuarkusConfigFactory.setConfig(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

    public void close() {
        //don't attempt to restart in the exit code handler
        restarting = true;
        if (codeGenWatcher != null) {
            codeGenWatcher.shutdown();
        }

        for (int i = listeners.size() - 1; i >= 0; i--) {
            try {
                listeners.get(i).beforeShutdown();
            } catch (Exception e) {
                log.warn("Unable to invoke 'beforeShutdown' of " + listeners.get(i).getClass(), e);
            }
        }

        try {
            stop();
            if (RuntimeUpdatesProcessor.INSTANCE == null) {
                throw new IllegalStateException(
                        "Hot deployment of the application is not supported when updating the Quarkus version. The application needs to be stopped and dev mode started up again");
            }
        } finally {
            try {
                if (RuntimeUpdatesProcessor.INSTANCE != null) {
                    try {
                        RuntimeUpdatesProcessor.INSTANCE.close();
                    } catch (IOException e) {
                        log.error("Failed to close compiler", e);
                    } finally {
                        RuntimeUpdatesProcessor.INSTANCE = null;
                    }
                }
                for (HotReplacementSetup i : hotReplacementSetups) {
                    i.close();
                }
            } finally {
                try {
                    DevConsoleManager.close();
                    curatedApplication.close();
                } finally {
                    if (shutdownThread != null) {
                        try {
                            Runtime.getRuntime().removeShutdownHook(shutdownThread);
                        } catch (IllegalStateException ignore) {

                        }
                        shutdownThread = null;
                    }
                    shutdownLatch.countDown();
                }
            }
        }
    }

    //the main entry point, but loaded inside the augmentation class loader
    @Override
    public void accept(CuratedApplication o, Map<String, Object> params) {
        //setup the dev mode thread pool for NIO
        System.setProperty("java.nio.channels.DefaultThreadPool.threadFactory",
                "io.quarkus.dev.io.NioThreadPoolThreadFactory");
        Timing.staticInitStarted(o.getBaseRuntimeClassLoader(), false);
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

            Object potentialContext = params.get(DevModeContext.class.getName());
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
                    List.of(new Consumer<BuildChainBuilder>() {
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
                                                    .getElementsWithResource(s.replace('.', '/') + ".class", true);
                                            return !res.isEmpty();
                                        }
                                    }));
                                }
                            }).produces(ApplicationClassPredicateBuildItem.class).build();
                        }
                    }),
                    List.of());

            // code generators should be initialized before the runtime compilation is setup to properly configure the sources directories
            codeGenWatcher = new CodeGenWatcher(curatedApplication, context);

            RuntimeUpdatesProcessor.INSTANCE = setupRuntimeCompilation(context, (Path) params.get(APP_ROOT),
                    (DevModeType) params.get(DevModeType.class.getName()));
            if (RuntimeUpdatesProcessor.INSTANCE != null) {
                RuntimeUpdatesProcessor.INSTANCE.checkForFileChange();
                RuntimeUpdatesProcessor.INSTANCE.checkForChangedClasses(true);
            }

            firstStart();

            //        doStart(false, Collections.emptySet());
            if (deploymentProblem != null || RuntimeUpdatesProcessor.INSTANCE.getCompileProblem() != null) {
                if (context.isAbortOnFailedStart()) {
                    Throwable throwable = deploymentProblem == null ? RuntimeUpdatesProcessor.INSTANCE.getCompileProblem()
                            : deploymentProblem;

                    throw (throwable instanceof RuntimeException ? (RuntimeException) throwable
                            : new RuntimeException(throwable));
                }
            }
            shutdownThread = new Thread(new Runnable() {
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
            }, "Quarkus Shutdown Thread");
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        } catch (Exception e) {
            RuntimeException toThrow = (e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e));
            try {
                close();
            } catch (IllegalStateException x) {
                // not sure which is the most important, but let's not silence the original exception
                toThrow.addSuppressed(x);
            }
            throw toThrow;
        }
    }
}
