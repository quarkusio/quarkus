package io.quarkus.deployment.dev;

import static io.quarkus.deployment.dev.testing.MessageFormat.BLUE;
import static java.util.Collections.singleton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
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
import io.quarkus.deployment.CodeGenerator;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.codegen.CodeGenData;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.dev.testing.MessageFormat;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.deployment.steps.ClassTransformingBuildStep;
import io.quarkus.deployment.util.FSWatchUtil;
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
    private final FSWatchUtil fsWatchUtil = new FSWatchUtil();
    private static volatile ConsoleStateManager.ConsoleContext consoleContext;

    private synchronized void firstStart(QuarkusClassLoader deploymentClassLoader, List<CodeGenData> codeGens) {

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            boolean augmentDone = false;
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
                                        }),
                                        new ConsoleCommand('e', "Edits the command line parameters and restarts",
                                                "to edit command line args (currently '" + MessageFormat.GREEN
                                                        + String.join(" ", context.getArgs()) + MessageFormat.RESET + "')",
                                                100, new ConsoleCommand.HelpState(() -> BLUE,
                                                        () -> String.join(" ", context.getArgs())),
                                                new Consumer<String>() {
                                                    @Override
                                                    public void accept(String args) {
                                                        try {
                                                            context.setArgs(
                                                                    CommandLineUtils.translateCommandline(args));
                                                        } catch (CommandLineException e) {
                                                            log.error("Failed to parse command line", e);
                                                            return;
                                                        }
                                                        consoleContext.reset();
                                                        RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
                                                    }
                                                }));

                            }
                        });

                startCodeGenWatcher(deploymentClassLoader, codeGens, context.getBuildSystemProperties());
                augmentDone = true;
                runner = start.runMainClass(context.getArgs());
                firstStartCompleted = true;
            } catch (Throwable t) {
                Throwable rootCause = t;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                if (!(rootCause instanceof BindException)) {
                    deploymentProblem = t;
                    if (!augmentDone) {
                        log.error("Failed to start quarkus", t);
                    }
                    if (!context.isAbortOnFailedStart()) {
                        //we need to set this here, while we still have the correct TCCL
                        //this is so the config is still valid, and we can read HTTP config from application.properties
                        log.info(
                                "Attempting to start live reload endpoint to recover from previous Quarkus startup failure");
                        if (RuntimeUpdatesProcessor.INSTANCE != null) {
                            Thread.currentThread().setContextClassLoader(curatedApplication.getBaseRuntimeClassLoader());
                            try {
                                if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                                    Class<?> cl = Thread.currentThread().getContextClassLoader()
                                            .loadClass(LoggingSetupRecorder.class.getName());
                                    cl.getMethod("handleFailedStart").invoke(null);
                                }
                                RuntimeUpdatesProcessor.INSTANCE.startupFailed();
                            } catch (Exception e) {
                                close();
                                log.error("Failed to recover after failed start", e);
                                //this is the end of the road, we just exit
                                //generally we only hit this if something is already listening on the HTTP port
                                //or the system config is so broken we can't start HTTP
                                System.exit(1);
                            }
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private void startCodeGenWatcher(QuarkusClassLoader classLoader, List<CodeGenData> codeGens,
            Map<String, String> properties) {

        Collection<FSWatchUtil.Watcher> watchers = new ArrayList<>();
        for (CodeGenData codeGen : codeGens) {
            watchers.add(new FSWatchUtil.Watcher(codeGen.sourceDir, codeGen.provider.inputExtension(),
                    modifiedPaths -> {
                        try {
                            CodeGenerator.trigger(classLoader,
                                    codeGen,
                                    curatedApplication.getAppModel(), properties);
                        } catch (Exception any) {
                            log.warn("Code generation failed", any);
                        }
                    }));
        }
        fsWatchUtil.observe(watchers, 500);
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
                firstStartCompleted = true;
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
            if (devModeType == DevModeType.LOCAL && context.getApplicationRoot().getTest().isPresent()) {
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
        fsWatchUtil.shutdown();
        try {
            stop();
        } finally {
            try {
                try {
                    RuntimeUpdatesProcessor.INSTANCE.close();
                } catch (IOException e) {
                    log.error("Failed to close compiler", e);
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
                                                    .getElementsWithResource(s.replace('.', '/') + ".class", true);
                                            return !res.isEmpty();
                                        }
                                    }));
                                }
                            }).produces(ApplicationClassPredicateBuildItem.class).build();
                        }
                    }),
                    Collections.emptyList());
            List<CodeGenData> codeGens = new ArrayList<>();
            QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();

            for (DevModeContext.ModuleInfo module : context.getAllModules()) {
                if (!module.getSourceParents().isEmpty() && module.getPreBuildOutputDir() != null) { // it's null for remote dev
                    codeGens.addAll(
                            CodeGenerator.init(
                                    deploymentClassLoader,
                                    module.getSourceParents(),
                                    Paths.get(module.getPreBuildOutputDir()),
                                    Paths.get(module.getTargetDir()),
                                    sourcePath -> module.addSourcePaths(singleton(sourcePath.toAbsolutePath().toString()))));
                }
            }
            RuntimeUpdatesProcessor.INSTANCE = setupRuntimeCompilation(context, (Path) params.get(APP_ROOT),
                    (DevModeType) params.get(DevModeType.class.getName()));
            if (RuntimeUpdatesProcessor.INSTANCE != null) {
                RuntimeUpdatesProcessor.INSTANCE.checkForFileChange();
                RuntimeUpdatesProcessor.INSTANCE.checkForChangedClasses(true);
            }
            firstStart(deploymentClassLoader, codeGens);

            //        doStart(false, Collections.emptySet());
            if (deploymentProblem != null || RuntimeUpdatesProcessor.INSTANCE.getCompileProblem() != null) {
                if (context.isAbortOnFailedStart()) {
                    throw new RuntimeException(
                            deploymentProblem == null ? RuntimeUpdatesProcessor.INSTANCE.getCompileProblem()
                                    : deploymentProblem);
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
            throw new RuntimeException(e);
        }
    }
}
