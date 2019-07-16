package io.quarkus.dev;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
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
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Timing;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The main entry point for the dev mojo execution
 */
public class DevModeMain {

    public static final String DEV_MODE_CONTEXT = "META-INF/dev-mode-context.dat";
    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private static volatile ClassLoader currentAppClassLoader;
    private static volatile URLClassLoader runtimeCl;
    private static List<File> classesRoots;
    private static File wiringDir;
    private static File cacheDir;
    private static DevModeContext context;

    private static Closeable runner;
    static volatile Throwable deploymentProblem;
    static volatile Throwable compileProblem;
    static RuntimeUpdatesProcessor runtimeUpdatesProcessor;

    static final Map<Class<?>, Object> liveReloadContext = new ConcurrentHashMap<>();

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();

        try (InputStream devModeCp = DevModeMain.class.getClassLoader().getResourceAsStream(DEV_MODE_CONTEXT)) {
            context = (DevModeContext) new ObjectInputStream(new DataInputStream(devModeCp)).readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //propagate system props
        for (Map.Entry<String, String> i : context.getSystemProperties().entrySet()) {
            if (!System.getProperties().containsKey(i.getKey())) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }

        // we can potentially have multiple roots separated by commas
        final String[] classesRootsParts = args[0].split(",");
        classesRoots = new ArrayList<>(classesRootsParts.length);
        for (String classesRootsPart : classesRootsParts) {
            classesRoots.add(new File(classesRootsPart));
        }

        wiringDir = new File(args[1]);
        cacheDir = new File(args[2]);

        runtimeUpdatesProcessor = RuntimeCompilationSetup.setup(context);
        if (runtimeUpdatesProcessor != null) {
            runtimeUpdatesProcessor.checkForChangedClasses();
        }
        //TODO: we can't handle an exception on startup with hot replacement, as Undertow might not have started

        doStart(false, Collections.emptySet());
        if (deploymentProblem != null || compileProblem != null) {
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

        LockSupport.park();
    }

    private static synchronized void doStart(boolean liveReload, Set<String> changedResources) {
        try {
            final URL[] urls = new URL[classesRoots.size()];
            for (int i = 0; i < classesRoots.size(); i++) {
                urls[i] = classesRoots.get(i).toURI().toURL();
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
                        .setTarget(classesRoots.get(0).toPath())
                        .setFrameworkClassesPath(wiringDir.toPath())
                        .setTransformerCache(cacheDir.toPath());

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

    public static synchronized void restartApp(Set<String> changedResources) {
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
}
