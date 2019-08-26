package io.quarkus.dev;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Timing;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The main entry point for the dev mojo execution
 */
public class DevModeMain implements Closeable {

    public static final String DEV_MODE_CONTEXT = "META-INF/dev-mode-context.dat";
    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private static volatile ClassLoader currentAppClassLoader;
    private static volatile URLClassLoader runtimeCl;
    private final DevModeContext context;

    private static Closeable runner;
    static volatile Throwable deploymentProblem;
    static volatile Throwable compileProblem;
    static RuntimeUpdatesProcessor runtimeUpdatesProcessor;
    private List<HotReplacementSetup> hotReplacement = new ArrayList<>();

    private final Map<Class<?>, Object> liveReloadContext = new ConcurrentHashMap<>();

    public DevModeMain(DevModeContext context) {
        this.context = context;
    }

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();

        try (InputStream devModeCp = DevModeMain.class.getClassLoader().getResourceAsStream(DEV_MODE_CONTEXT)) {
            DevModeContext context = (DevModeContext) new ObjectInputStream(new DataInputStream(devModeCp)).readObject();
            new DevModeMain(context).start();

            LockSupport.park();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {

        }
    }

    public void start() throws Exception {
        //propagate system props
        for (Map.Entry<String, String> i : context.getSystemProperties().entrySet()) {
            if (!System.getProperties().containsKey(i.getKey())) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }

        for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class)) {
            hotReplacement.add(service);
        }

        runtimeUpdatesProcessor = setupRuntimeCompilation(context);
        if (runtimeUpdatesProcessor != null) {
            runtimeUpdatesProcessor.checkForChangedClasses();
        }
        //TODO: we can't handle an exception on startup with hot replacement, as Undertow might not have started

        doStart(false, Collections.emptySet());
        if (deploymentProblem != null || compileProblem != null) {
            if (context.isAbortOnFailedStart()) {
                throw new RuntimeException(deploymentProblem == null ? compileProblem : deploymentProblem);
            }
            log.error("Failed to start Quarkus, attempting to start hot replacement endpoint to recover");
            if (runtimeUpdatesProcessor != null) {
                runtimeUpdatesProcessor.startupFailed();
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DevModeMain.class) {
                    if (runner != null) {
                        try {
                            runner.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (runtimeCl != null) {
                        try {
                            runtimeCl.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "Quarkus Shutdown Thread"));

    }

    private synchronized void doStart(boolean liveReload, Set<String> changedResources) {
        try {
            final URL[] urls = new URL[context.getClassesRoots().size()];
            for (int i = 0; i < context.getClassesRoots().size(); i++) {
                urls[i] = context.getClassesRoots().get(i).toURI().toURL();
            }
            runtimeCl = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            currentAppClassLoader = runtimeCl;
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            //we can potentially throw away this class loader, and reload the app
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                RuntimeRunner.Builder builder = RuntimeRunner.builder()
                        .setLaunchMode(LaunchMode.DEVELOPMENT)
                        .setLiveReloadState(new LiveReloadBuildItem(liveReload, changedResources, liveReloadContext))
                        .setClassLoader(runtimeCl)
                        // just use the first item in classesRoot which is where the actual class files are written
                        .setTarget(context.getClassesRoots().get(0).toPath())
                        .setTransformerCache(context.getCacheDir().toPath());
                if (context.getFrameworkClassesDir() != null) {
                    builder.setFrameworkClassesPath(context.getFrameworkClassesDir().toPath());
                }

                List<Path> addAdditionalHotDeploymentPaths = new ArrayList<>();
                for (DevModeContext.ModuleInfo i : context.getModules()) {
                    if (i.getClassesPath() != null) {
                        Path classesPath = Paths.get(i.getClassesPath());
                        addAdditionalHotDeploymentPaths.add(classesPath);
                        builder.addAdditionalHotDeploymentPath(classesPath);
                    }
                }
                // Make it possible to identify wiring classes generated for classes from additional hot deployment paths
                builder.addChainCustomizer(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                context.produce(new ApplicationClassPredicateBuildItem(n -> {
                                    return getClassInApplicationClassPaths(n, addAdditionalHotDeploymentPaths) != null;
                                }));
                            }
                        }).produces(ApplicationClassPredicateBuildItem.class).build();
                    }
                });

                Properties buildSystemProperties = new Properties();
                buildSystemProperties.putAll(context.getBuildSystemProperties());
                builder.setBuildSystemProperties(buildSystemProperties);

                RuntimeRunner runner = builder
                        .build();
                runner.run();
                DevModeMain.runner = runner;
                deploymentProblem = null;
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Throwable t) {
            deploymentProblem = t;
            log.error("Failed to start quarkus", t);
        }
    }

    public synchronized void restartApp(Set<String> changedResources) {
        stop();
        Timing.restart();
        doStart(true, changedResources);
    }

    public static ClassLoader getCurrentAppClassLoader() {
        return currentAppClassLoader;
    }

    private static Path getClassInApplicationClassPaths(String name, List<Path> addAdditionalHotDeploymentPaths) {
        final String fileName = name.replace('.', '/') + ".class";
        Path classLocation;
        for (Path i : addAdditionalHotDeploymentPaths) {
            classLocation = i.resolve(fileName);
            if (Files.exists(classLocation)) {
                return classLocation;
            }
        }
        return null;
    }

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context) throws Exception {
        if (!context.getModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            ClassLoaderCompiler compiler;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(),
                        compilationProviders, context);
            } catch (Exception e) {
                log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(context, compiler, this);

            for (HotReplacementSetup service : hotReplacement) {
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
            Thread.currentThread().setContextClassLoader(runtimeCl);
            try {
                runner.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        SmallRyeConfigProviderResolver.instance().releaseConfig(SmallRyeConfigProviderResolver.instance().getConfig());
        DevModeMain.runner = null;
    }

    public void close() {
        try {
            stop();
        } finally {
            for (HotReplacementSetup i : hotReplacement) {
                i.close();
            }
        }
    }
}
