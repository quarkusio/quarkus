package io.quarkus.dev;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public class IsolatedDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>>, Closeable {

    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private DevModeContext context;

    private final List<HotReplacementSetup> hotReplacementSetups = new ArrayList<>();
    private static volatile RunningQuarkusApplication runner;
    static volatile Throwable deploymentProblem;
    static volatile Throwable compileProblem;
    static volatile RuntimeUpdatesProcessor runtimeUpdatesProcessor;
    private static volatile CuratedApplication curatedApplication;
    private static volatile AugmentAction augmentAction;

    private synchronized void firstStart() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.createInitialRuntimeApplication();
                runner = start.run();
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
        stop();
        Timing.restart(curatedApplication.getAugmentClassLoader());
        deploymentProblem = null;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.reloadExistingApplication(changedResources);
                runner = start.run();
            } catch (Throwable t) {
                deploymentProblem = t;
                log.error("Failed to start quarkus", t);

            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context, CuratedApplication application)
            throws Exception {
        if (!context.getModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            ClassLoaderCompiler compiler;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(), curatedApplication,
                        compilationProviders, context);
            } catch (Exception e) {
                log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(context, compiler, this);

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
        final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable ignored) {
            // just means no config was installed, which is fine
        }
        runner = null;
    }

    public void close() {
        try {
            stop();
        } finally {
            try {
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
                                            for (AdditionalDependency i : curatedApplication.getQuarkusBootstrap()
                                                    .getAdditionalApplicationArchives()) {
                                                if (i.isHotReloadable()) {
                                                    Path p = i.getArchivePath().resolve(s.replace(".", "/") + ".class");
                                                    if (Files.exists(p)) {
                                                        return true;
                                                    }
                                                }
                                            }
                                            return false;
                                        }
                                    }));
                                }
                            }).produces(ApplicationClassPredicateBuildItem.class).build();
                        }
                    }));
            runtimeUpdatesProcessor = setupRuntimeCompilation(context, o);
            if (runtimeUpdatesProcessor != null) {
                runtimeUpdatesProcessor.checkForFileChange();
                runtimeUpdatesProcessor.checkForChangedClasses();
            }
            firstStart();

            //        doStart(false, Collections.emptySet());
            if (deploymentProblem != null || compileProblem != null) {
                if (context.isAbortOnFailedStart()) {
                    throw new RuntimeException(deploymentProblem == null ? compileProblem : deploymentProblem);
                }
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (DevModeMain.class) {
                        if (runner != null) {
                            try {
                                runner.close();
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
