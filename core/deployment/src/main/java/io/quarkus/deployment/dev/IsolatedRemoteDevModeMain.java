package io.quarkus.deployment.dev;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.deployment.dev.remote.DefaultRemoteDevClient;
import io.quarkus.deployment.dev.remote.RemoteDevClient;
import io.quarkus.deployment.dev.remote.RemoteDevClientProvider;
import io.quarkus.deployment.mutability.DevModeTask;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.steps.JarResultBuildStep;
import io.quarkus.deployment.steps.ClassTransformingBuildStep;
import io.quarkus.dev.spi.DeploymentFailedStartHandler;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.quarkus.runtime.util.HashUtil;

/**
 * The main entry point for the local (developer side) of remote dev mode
 */
public class IsolatedRemoteDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>>, Closeable {

    private static final Logger log = Logger.getLogger(IsolatedRemoteDevModeMain.class);

    private volatile DevModeContext context;

    private final List<HotReplacementSetup> hotReplacementSetups = new ArrayList<>();
    static volatile Throwable deploymentProblem;
    static volatile RemoteDevClient remoteDevClient;
    static volatile Closeable remoteDevClientSession;
    private static volatile CuratedApplication curatedApplication;
    private static volatile AugmentAction augmentAction;
    private static volatile Map<String, String> currentHashes;
    private static volatile Path appRoot;
    private static volatile Map<DevModeContext.ModuleInfo, Set<String>> copiedStaticResources = new HashMap<>();

    static RemoteDevClient createClient(CuratedApplication curatedApplication) {
        ServiceLoader<RemoteDevClientProvider> providers = ServiceLoader.load(RemoteDevClientProvider.class,
                curatedApplication.getAugmentClassLoader());
        RemoteDevClient client = null;
        for (RemoteDevClientProvider provider : providers) {
            Optional<RemoteDevClient> opt = provider.getClient();
            if (opt.isPresent()) {
                client = opt.get();
                break;
            }
        }
        if (client == null) {
            client = new DefaultRemoteDevClient();
        }
        return client;
    }

    private synchronized JarResult generateApplication() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            //ok, we have resolved all the deps
            try {
                AugmentResult start = augmentAction.createProductionApplication();
                if (!start.getJar().getType().equalsIgnoreCase(PackageConfig.MUTABLE_JAR)) {
                    throw new RuntimeException(
                            "remote-dev can only be used with mutable applications generated with the fast-jar format");
                }
                //now extract the artifacts, to mirror the remote side
                DevModeTask.extractDevModeClasses(start.getJar().getPath().getParent(), curatedApplication.getAppModel(), null);
                return start.getJar();
            } catch (Throwable t) {
                deploymentProblem = t;
                log.error("Failed to generate Quarkus application", t);
                return null;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context, Path applicationRoot)
            throws Exception {
        if (!context.getAllModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getAllModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            QuarkusCompiler compiler;
            try {
                compiler = new QuarkusCompiler(curatedApplication, compilationProviders, context);
            } catch (Exception e) {
                log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            //this is never the remote side
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(applicationRoot, context, compiler,
                    DevModeType.REMOTE_LOCAL_SIDE, this::regenerateApplication,
                    new BiConsumer<DevModeContext.ModuleInfo, String>() {
                        @Override
                        public void accept(DevModeContext.ModuleInfo moduleInfo, String s) {
                            copiedStaticResources.computeIfAbsent(moduleInfo, ss -> new HashSet<>()).add(s);
                        }
                    }, new BiFunction<String, byte[], byte[]>() {
                        @Override
                        public byte[] apply(String s, byte[] bytes) {
                            return ClassTransformingBuildStep.transform(s, bytes);
                        }
                    }, null);

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
            return processor;
        }
        return null;
    }

    void regenerateApplication(Set<String> ignore, ClassScanResult ignore2) {
        generateApplication();
    }

    public void close() {
        try {
            try {
                RuntimeUpdatesProcessor.INSTANCE.close();
            } catch (IOException e) {
                log.error("Failed to close compiler", e);
            }
            for (HotReplacementSetup i : hotReplacementSetups) {
                i.close();
            }
            if (remoteDevClientSession != null) {
                try {
                    remoteDevClientSession.close();
                } catch (IOException e) {
                    log.error("Failed to close client", e);
                }
            }
        } finally {
            curatedApplication.close();
        }

    }

    //the main entry point, but loaded inside the augmentation class loader
    @Override
    public void accept(CuratedApplication o, Map<String, Object> o2) {
        LoggingSetupRecorder.handleFailedStart(); //we are not going to actually run an app
        Timing.staticInitStarted(o.getBaseRuntimeClassLoader(), false);
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

            augmentAction = new AugmentActionImpl(curatedApplication);
            RuntimeUpdatesProcessor.INSTANCE = setupRuntimeCompilation(context, appRoot);

            if (RuntimeUpdatesProcessor.INSTANCE != null) {
                RuntimeUpdatesProcessor.INSTANCE.checkForFileChange();
                RuntimeUpdatesProcessor.INSTANCE.checkForChangedClasses(true);
            }

            JarResult result = generateApplication();
            if (result != null) {
                appRoot = result.getPath().getParent();
                currentHashes = createHashes(appRoot);
            }

            remoteDevClient = createClient(curatedApplication);
            remoteDevClientSession = doConnect();

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (DevModeMain.class) {
                        try {
                            close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "Quarkus Shutdown Thread"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Closeable doConnect() {
        return remoteDevClient.sendConnectRequest(new RemoteDevState(currentHashes, deploymentProblem),
                new Function<Set<String>, Map<String, byte[]>>() {
                    @Override
                    public Map<String, byte[]> apply(Set<String> fileNames) {
                        Map<String, byte[]> ret = new HashMap<>();
                        for (String i : fileNames) {
                            try {
                                Path resolvedPath = appRoot.resolve(i);
                                if (!Files.isDirectory(resolvedPath)) {
                                    ret.put(i, Files.readAllBytes(resolvedPath));
                                }
                            } catch (IOException e) {
                                log.error("Failed to read file " + i, e);
                            }
                        }
                        return ret;
                    }
                }, new Supplier<RemoteDevClient.SyncResult>() {
                    @Override
                    public RemoteDevClient.SyncResult get() {
                        return runSync();
                    }
                });
    }

    private RemoteDevClient.SyncResult runSync() {
        //do hot reload stuff
        Set<String> removed = new HashSet<>();
        Map<String, byte[]> changed = new HashMap<>();
        try {
            boolean scanResult = RuntimeUpdatesProcessor.INSTANCE.doScan(true);
            if (!scanResult && !copiedStaticResources.isEmpty()) {
                scanResult = true;
                regenerateApplication(Collections.emptySet(), new ClassScanResult());
            }
            copiedStaticResources.clear();
            if (scanResult) {
                Map<String, String> newHashes = createHashes(appRoot);
                Set<String> allKeys = new HashSet<>(newHashes.keySet());
                allKeys.addAll(currentHashes.keySet());
                for (String key : allKeys) {
                    String newHash = newHashes.get(key);
                    String oldHash = currentHashes.get(key);
                    if (newHash == null) {
                        removed.add(key);
                    } else if (!Objects.equals(newHash, oldHash)) {
                        changed.put(key, Files.readAllBytes(appRoot.resolve(key)));
                    }
                }
                currentHashes = newHashes;
            }
        } catch (IOException e) {
            deploymentProblem = e;
        }
        return new RemoteDevClient.SyncResult() {
            @Override
            public Map<String, byte[]> getChangedFiles() {
                return changed;
            }

            @Override
            public Set<String> getRemovedFiles() {
                return removed;
            }

            @Override
            public Throwable getProblem() {
                return RuntimeUpdatesProcessor.INSTANCE.getDeploymentProblem();
            }

        };
    }

    static Map<String, String> createHashes(Path appRoot) throws IOException {
        Path quarkus = appRoot.resolve(JarResultBuildStep.QUARKUS); //we filter this jar, it has no relevance for remote dev
        Map<String, String> hashes = new HashMap<>();
        Files.walkFileTree(appRoot, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(quarkus)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                hashes.put(appRoot.relativize(file).toString().replace('\\', '/'),
                        HashUtil.sha1(Files.readAllBytes(file)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return hashes;
    }
}
