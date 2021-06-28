package io.quarkus.deployment.dev;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassDefinition;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.changeagent.ClassChangeAgent;
import io.quarkus.deployment.dev.filewatch.FileChangeCallback;
import io.quarkus.deployment.dev.filewatch.FileChangeEvent;
import io.quarkus.deployment.dev.filewatch.WatchServiceFileSystemWatcher;
import io.quarkus.deployment.dev.testing.TestListener;
import io.quarkus.deployment.dev.testing.TestRunner;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.dev.testing.TestScanningLock;

public class RuntimeUpdatesProcessor implements HotReplacementContext, Closeable {
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("linux");

    private static final Logger log = Logger.getLogger(RuntimeUpdatesProcessor.class);

    private static final String CLASS_EXTENSION = ".class";

    public static volatile RuntimeUpdatesProcessor INSTANCE;

    private final Path applicationRoot;
    private final DevModeContext context;
    private final QuarkusCompiler compiler;
    private final DevModeType devModeType;
    volatile Throwable compileProblem;

    private volatile Predicate<ClassInfo> disableInstrumentationForClassPredicate = new AlwaysFalsePredicate<>();
    private volatile Predicate<Index> disableInstrumentationForIndexPredicate = new AlwaysFalsePredicate<>();

    private static volatile boolean instrumentationLogPrinted = false;
    /**
     * dev mode replacement and test running track their changes separately
     */
    private final TimestampSet main = new TimestampSet();
    private final TimestampSet test = new TimestampSet();
    final Map<Path, Long> sourceFileTimestamps = new ConcurrentHashMap<>();

    private final List<Runnable> preScanSteps = new CopyOnWriteArrayList<>();
    private final List<Consumer<Set<String>>> noRestartChangesConsumers = new CopyOnWriteArrayList<>();
    private final List<HotReplacementSetup> hotReplacementSetup = new ArrayList<>();
    private final BiConsumer<Set<String>, ClassScanResult> restartCallback;
    private final BiConsumer<DevModeContext.ModuleInfo, String> copyResourceNotification;
    private final BiFunction<String, byte[], byte[]> classTransformers;
    private final ReentrantLock scanLock = new ReentrantLock();

    /**
     * The index for the last successful start. Used to determine if the class has changed its structure
     * and determine if it is eligible for an instrumentation based reload.
     */
    private static volatile IndexView lastStartIndex;

    /**
     * Resources that appear in both src and target, these will be removed if the src resource subsequently disappears.
     * This map contains the paths in the target dir, one for each module, otherwise on a second module we will delete files
     * from the first one
     */
    private final Map<DevModeContext.CompilationUnit, Set<Path>> correspondingResources = new ConcurrentHashMap<>();

    private final TestSupport testSupport;
    private volatile boolean firstTestScanComplete;
    private volatile Boolean instrumentationEnabled;
    private volatile boolean liveReloadEnabled = true;

    private WatchServiceFileSystemWatcher testClassChangeWatcher;
    private Timer testClassChangeTimer;

    public RuntimeUpdatesProcessor(Path applicationRoot, DevModeContext context, QuarkusCompiler compiler,
            DevModeType devModeType, BiConsumer<Set<String>, ClassScanResult> restartCallback,
            BiConsumer<DevModeContext.ModuleInfo, String> copyResourceNotification,
            BiFunction<String, byte[], byte[]> classTransformers,
            TestSupport testSupport) {
        this.applicationRoot = applicationRoot;
        this.context = context;
        this.compiler = compiler;
        this.devModeType = devModeType;
        this.restartCallback = restartCallback;
        this.copyResourceNotification = copyResourceNotification;
        this.classTransformers = classTransformers;
        this.testSupport = testSupport;
        if (testSupport != null) {
            testSupport.addListener(new TestListener() {
                @Override
                public void testsEnabled() {
                    if (!firstTestScanComplete) {
                        checkForChangedTestClasses(true);
                        firstTestScanComplete = true;
                    }
                    startTestScanningTimer();
                }

                @Override
                public void testsDisabled() {
                    synchronized (RuntimeUpdatesProcessor.this) {
                        if (testClassChangeWatcher != null) {
                            try {
                                testClassChangeWatcher.close();
                            } catch (IOException e) {
                                //ignore
                            }
                            testClassChangeWatcher = null;
                        }
                        if (testClassChangeTimer != null) {
                            testClassChangeTimer.cancel();
                            testClassChangeTimer = null;
                        }
                    }
                }
            });
        }
    }

    public TestSupport getTestSupport() {
        return testSupport;
    }

    @Override
    public Path getClassesDir() {
        //TODO: fix all these
        for (DevModeContext.ModuleInfo i : context.getAllModules()) {
            return Paths.get(i.getMain().getClassesPath());
        }
        return null;
    }

    @Override
    public List<Path> getSourcesDir() {
        return context.getAllModules().stream().flatMap(m -> m.getMain().getSourcePaths().toList().stream())
                .collect(toList());
    }

    private void startTestScanningTimer() {
        synchronized (this) {
            if (testClassChangeWatcher == null && testClassChangeTimer == null) {
                if (IS_LINUX) {
                    //note that this is only used for notifications that something has changed,
                    //this triggers the same file scan as the polling approach
                    //this is not as efficient as it could be, but saves having two separate code paths
                    testClassChangeWatcher = new WatchServiceFileSystemWatcher("Quarkus Test Watcher", true);
                    FileChangeCallback callback = new FileChangeCallback() {
                        @Override
                        public void handleChanges(Collection<FileChangeEvent> changes) {
                            //sometimes changes come through as two events
                            //which can cause problems for our CI tests
                            //and cause unessesary runs.
                            //we add a half second delay for CI tests, to make sure this does not cause
                            //problems
                            try {
                                if (context.isTest()) {
                                    Thread.sleep(500);
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            periodicTestCompile();
                        }
                    };
                    for (DevModeContext.ModuleInfo module : context.getAllModules()) {
                        for (Path path : module.getMain().getSourcePaths()) {
                            testClassChangeWatcher.watchPath(path.toFile(), callback);
                        }
                        for (Path path : module.getMain().getResourcePaths()) {
                            testClassChangeWatcher.watchPath(path.toFile(), callback);
                        }
                    }
                    for (Path path : context.getApplicationRoot().getTest().get().getSourcePaths()) {
                        testClassChangeWatcher.watchPath(path.toFile(), callback);
                    }
                    for (Path path : context.getApplicationRoot().getTest().get().getResourcePaths()) {
                        testClassChangeWatcher.watchPath(path.toFile(), callback);
                    }
                    periodicTestCompile();
                } else {
                    testClassChangeTimer = new Timer("Test Compile Timer", true);
                    testClassChangeTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            periodicTestCompile();
                        }
                    }, 1, 1000);
                }
            }
        }
    }

    private void periodicTestCompile() {
        scanLock.lock();
        TestScanningLock.lockForTests();
        try {
            ClassScanResult changedTestClassResult = compileTestClasses();
            ClassScanResult changedApp = checkForChangedClasses(compiler, DevModeContext.ModuleInfo::getMain, false, test);
            Set<String> filesChanges = new HashSet<>(checkForFileChange(s -> s.getTest().get(), test));
            filesChanges.addAll(checkForFileChange(DevModeContext.ModuleInfo::getMain, test));
            boolean configFileRestartNeeded = filesChanges.stream().map(test.watchedFilePaths::get)
                    .anyMatch(Boolean.TRUE::equals);

            ClassScanResult merged = ClassScanResult.merge(changedTestClassResult, changedApp);
            if (configFileRestartNeeded) {
                if (compileProblem != null) {
                    testSupport.getTestRunner().testCompileFailed(compileProblem);
                } else {
                    testSupport.getTestRunner().runTests(null);
                }
            } else if (merged.isChanged()) {
                if (compileProblem != null) {
                    testSupport.getTestRunner().testCompileFailed(compileProblem);
                } else {
                    testSupport.getTestRunner().runTests(merged);
                }
            }
        } finally {
            TestScanningLock.unlockForTests();
            scanLock.unlock();
        }
    }

    private ClassScanResult compileTestClasses() {
        QuarkusCompiler testCompiler = testSupport.getCompiler();
        TestRunner testRunner = testSupport.getTestRunner();
        ClassScanResult changedTestClassResult = new ClassScanResult();
        try {
            changedTestClassResult = checkForChangedClasses(testCompiler,
                    m -> m.getTest().orElse(DevModeContext.EMPTY_COMPILATION_UNIT), false, test);
            if (compileProblem != null) {
                testRunner.testCompileFailed(compileProblem);
                compileProblem = null; //we don't want to block the app over a test problem
            } else {
                if (changedTestClassResult.isChanged()) {
                    testRunner.testCompileSucceeded();
                }
            }
        } catch (Throwable e) {
            testRunner.testCompileFailed(e);
        }
        return changedTestClassResult;
    }

    @Override
    public List<Path> getResourcesDir() {
        List<Path> ret = new ArrayList<>();
        for (DevModeContext.ModuleInfo i : context.getAllModules()) {
            if (!i.getMain().getResourcePaths().isEmpty()) {
                for (Path path : i.getMain().getResourcePaths()) {
                    ret.add(path);
                }
            } else if (i.getMain().getResourcesOutputPath() != null) {
                ret.add(Paths.get(i.getMain().getResourcesOutputPath()));
            }
        }
        Collections.reverse(ret); //make sure the actual project is before dependencies
        return ret;
    }

    @Override
    public Throwable getDeploymentProblem() {
        //we differentiate between these internally, however for the error reporting they are the same
        return compileProblem != null ? compileProblem
                : IsolatedDevModeMain.deploymentProblem;
    }

    @Override
    public void setRemoteProblem(Throwable throwable) {
        compileProblem = throwable;
    }

    @Override
    public void updateFile(String file, byte[] data) {
        if (file.startsWith("/")) {
            file = file.substring(1);
        }
        try {
            Path resolve = applicationRoot.resolve(file);
            if (!Files.exists(resolve.getParent())) {
                Files.createDirectories(resolve.getParent());
            }
            Files.write(resolve, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isTest() {
        return context.isTest();
    }

    @Override
    public DevModeType getDevModeType() {
        return devModeType;
    }

    @Override
    public boolean doScan(boolean userInitiated) throws IOException {
        return doScan(userInitiated, false);
    }

    public boolean doScan(boolean userInitiated, boolean forceRestart) throws IOException {
        if (!liveReloadEnabled && !forceRestart) {
            return false;
        }
        scanLock.lock();
        try {
            if (testSupport != null) {
                testSupport.pause();
            }
            final long startNanoseconds = System.nanoTime();
            for (Runnable step : preScanSteps) {
                try {
                    step.run();
                } catch (Throwable t) {
                    log.error("Pre Scan step failed", t);
                }
            }

            ClassScanResult changedClassResults = checkForChangedClasses(compiler, DevModeContext.ModuleInfo::getMain, false,
                    main);
            Set<String> filesChanged = checkForFileChange(DevModeContext.ModuleInfo::getMain, main);

            boolean configFileRestartNeeded = forceRestart || filesChanged.stream().map(main.watchedFilePaths::get)
                    .anyMatch(Boolean.TRUE::equals);
            boolean instrumentationChange = false;

            List<Path> changedFilesForRestart = new ArrayList<>();
            if (configFileRestartNeeded) {
                changedFilesForRestart
                        .addAll(filesChanged.stream().filter(fn -> Boolean.TRUE.equals(main.watchedFilePaths.get(fn)))
                                .map(Paths::get).collect(Collectors.toList()));
            }
            changedFilesForRestart.addAll(changedClassResults.getChangedClasses());
            changedFilesForRestart.addAll(changedClassResults.getAddedClasses());
            changedFilesForRestart.addAll(changedClassResults.getDeletedClasses());

            if (ClassChangeAgent.getInstrumentation() != null && lastStartIndex != null && !configFileRestartNeeded
                    && devModeType != DevModeType.REMOTE_LOCAL_SIDE) {
                //attempt to do an instrumentation based reload
                //if only code has changed and not the class structure, then we can do a reload
                //using the JDK instrumentation API (assuming we were started with the javaagent)
                if (changedClassResults.deletedClasses.isEmpty()
                        && changedClassResults.addedClasses.isEmpty()
                        && !changedClassResults.changedClasses.isEmpty()) {
                    try {
                        Indexer indexer = new Indexer();
                        //attempt to use the instrumentation API
                        ClassDefinition[] defs = new ClassDefinition[changedClassResults.changedClasses.size()];
                        int index = 0;
                        for (Path i : changedClassResults.changedClasses) {
                            byte[] bytes = Files.readAllBytes(i);
                            String name = indexer.index(new ByteArrayInputStream(bytes)).name().toString();
                            defs[index++] = new ClassDefinition(Thread.currentThread().getContextClassLoader().loadClass(name),
                                    classTransformers.apply(name, bytes));
                        }
                        Index current = indexer.complete();
                        boolean ok = instrumentationEnabled()
                                && !disableInstrumentationForIndexPredicate.test(current);
                        if (ok) {
                            for (ClassInfo clazz : current.getKnownClasses()) {
                                ClassInfo old = lastStartIndex.getClassByName(clazz.name());
                                if (!ClassComparisonUtil.isSameStructure(clazz, old)
                                        || disableInstrumentationForClassPredicate.test(clazz)) {
                                    ok = false;
                                    break;
                                }
                            }
                        }

                        if (ok) {
                            log.info("Application restart not required, replacing classes via instrumentation");
                            ClassChangeAgent.getInstrumentation().redefineClasses(defs);
                            instrumentationChange = true;
                        }
                    } catch (Exception e) {
                        log.error("Failed to replace classes via instrumentation", e);
                        instrumentationChange = false;
                    }
                }
            }

            //if there is a deployment problem we always restart on scan
            //this is because we can't setup the config file watches
            //in an ideal world we would just check every resource file for changes, however as everything is already
            //all broken we just assume the reason that they have refreshed is because they have fixed something
            //trying to watch all resource files is complex and this is likely a good enough solution for what is already an edge case
            boolean restartNeeded = !instrumentationChange && (changedClassResults.isChanged()
                    || (IsolatedDevModeMain.deploymentProblem != null && userInitiated) || configFileRestartNeeded);
            if (restartNeeded) {
                String changeString = changedFilesForRestart.stream().map(Path::getFileName).map(Object::toString)
                        .collect(Collectors.joining(", ")) + ".";
                log.infof("Restarting quarkus due to changes in " + changeString);
                restartCallback.accept(filesChanged, changedClassResults);
                long timeNanoSeconds = System.nanoTime() - startNanoseconds;
                log.infof("Live reload total time: %ss ", Timing.convertToBigDecimalSeconds(timeNanoSeconds));
                if (TimeUnit.SECONDS.convert(timeNanoSeconds, TimeUnit.NANOSECONDS) >= 4 && !instrumentationEnabled()) {
                    if (!instrumentationLogPrinted) {
                        instrumentationLogPrinted = true;
                        log.info(
                                "Live reload took more than 4 seconds, you may want to enable instrumentation based reload (quarkus.live-reload.instrumentation=true). This allows small changes to take effect without restarting Quarkus.");
                    }
                }

                return true;
            } else if (!filesChanged.isEmpty()) {
                for (Consumer<Set<String>> consumer : noRestartChangesConsumers) {
                    try {
                        consumer.accept(filesChanged);
                    } catch (Throwable t) {
                        log.error("Changed files consumer failed", t);
                    }
                }
                log.infof("Files changed but restart not needed - notified extensions in: %ss ",
                        Timing.convertToBigDecimalSeconds(System.nanoTime() - startNanoseconds));
            } else if (instrumentationChange) {
                log.infof("Live reload performed via instrumentation, no restart needed, total time: %ss ",
                        Timing.convertToBigDecimalSeconds(System.nanoTime() - startNanoseconds));
            }
            return false;

        } finally {
            scanLock.unlock();
            if (testSupport != null) {
                testSupport.resume();
            }
        }
    }

    public boolean instrumentationEnabled() {
        if (instrumentationEnabled != null) {
            return instrumentationEnabled;
        }
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.live-reload.instrumentation", boolean.class).orElse(true);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void addPreScanStep(Runnable runnable) {
        preScanSteps.add(runnable);
    }

    @Override
    public void consumeNoRestartChanges(Consumer<Set<String>> consumer) {
        noRestartChangesConsumers.add(consumer);
    }

    @Override
    public Set<String> syncState(Map<String, String> fileHashes) {
        if (getDevModeType() != DevModeType.REMOTE_SERVER_SIDE) {
            throw new RuntimeException("Can only sync state on the server side of remote dev mode");
        }
        Set<String> ret = new HashSet<>();
        try {
            Map<String, String> ourHashes = new HashMap<>(IsolatedRemoteDevModeMain.createHashes(applicationRoot));
            for (Map.Entry<String, String> i : fileHashes.entrySet()) {
                String ours = ourHashes.remove(i.getKey());
                if (!Objects.equals(ours, i.getValue())) {
                    ret.add(i.getKey());
                }
            }
            for (Map.Entry<String, String> remaining : ourHashes.entrySet()) {
                String file = remaining.getKey();
                if (file.endsWith("META-INF/MANIFEST.MF") || file.contains("META-INF/maven")
                        || !file.contains("/")) {
                    //we have some filters, for files that we don't want to delete
                    continue;
                }
                log.info("Deleting removed file " + file);
                Files.deleteIfExists(applicationRoot.resolve(file));
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ClassScanResult checkForChangedClasses(boolean firstScan) {
        ClassScanResult classScanResult = checkForChangedClasses(compiler, DevModeContext.ModuleInfo::getMain, firstScan, main);
        if (firstScan) {
            test.merge(main);
        }
        return classScanResult;
    }

    ClassScanResult checkForChangedTestClasses(boolean firstScan) {
        if (!testSupport.isStarted()) {
            return new ClassScanResult();
        }
        ClassScanResult ret = checkForChangedClasses(testSupport.getCompiler(),
                s -> s.getTest().orElse(DevModeContext.EMPTY_COMPILATION_UNIT), firstScan,
                test);
        if (firstScan) {
            startTestScanningTimer();
        }
        return ret;
    }

    /**
     * A first scan is considered done when we have visited all modules at least once.
     * This is useful in two ways.
     * - To make sure that source time stamps have been recorded at least once
     * - To avoid re-compiling on first run by ignoring all first time changes detected by
     * {@link RuntimeUpdatesProcessor#checkIfFileModified(Path, Map, boolean, boolean)} during the first scan.
     */
    ClassScanResult checkForChangedClasses(QuarkusCompiler compiler,
            Function<DevModeContext.ModuleInfo, DevModeContext.CompilationUnit> cuf, boolean firstScan,
            TimestampSet timestampSet) {
        ClassScanResult classScanResult = new ClassScanResult();
        boolean ignoreFirstScanChanges = firstScan;

        for (DevModeContext.ModuleInfo module : context.getAllModules()) {
            final List<Path> moduleChangedSourceFilePaths = new ArrayList<>();

            for (Path sourcePath : cuf.apply(module).getSourcePaths()) {
                final Set<File> changedSourceFiles;
                Path start = sourcePath;
                if (!Files.exists(start)) {
                    continue;
                }
                try (final Stream<Path> sourcesStream = Files.walk(start)) {
                    changedSourceFiles = sourcesStream
                            .parallel()
                            .filter(p -> matchingHandledExtension(p).isPresent()
                                    && sourceFileWasRecentModified(p, ignoreFirstScanChanges, firstScan))
                            .map(Path::toFile)
                            //Needing a concurrent Set, not many standard options:
                            .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (!changedSourceFiles.isEmpty()) {
                    classScanResult.compilationHappened = true;
                    //so this is pretty yuck, but on a lot of systems a write is actually a truncate + write
                    //its possible we see the truncated file timestamp, then the write updates the timestamp
                    //which will then re-trigger continuous testing/live reload
                    //the empty fine does not normally cause issues as by the time we actually compile it the write
                    //has completed (but the old timestamp is used)
                    for (File i : changedSourceFiles) {
                        if (i.length() == 0) {
                            try {
                                //give time for the write to complete
                                //note that this is just 'best effort'
                                //the file time may have already been updated by the time we get here
                                Thread.sleep(200);
                                break;
                            } catch (InterruptedException e) {
                                //ignore
                            }
                        }
                    }
                    Map<File, Long> compileTimestamps = new HashMap<>();

                    //now we record the timestamps as they are before the compile phase
                    for (File i : changedSourceFiles) {
                        compileTimestamps.put(i, i.lastModified());
                    }
                    for (;;) {
                        try {
                            final Set<Path> changedPaths = changedSourceFiles.stream()
                                    .map(File::toPath)
                                    .collect(Collectors.toSet());
                            moduleChangedSourceFilePaths.addAll(changedPaths);
                            compiler.compile(sourcePath.toString(), changedSourceFiles.stream()
                                    .collect(groupingBy(this::getFileExtension, Collectors.toSet())));
                            compileProblem = null;
                        } catch (Exception e) {
                            compileProblem = e;
                            return new ClassScanResult();
                        }
                        boolean timestampsChanged = false;
                        //check to make sure no changes have occurred while the compilation was
                        //taking place. If they have changed we update the timestamp in the compile
                        //time set, and re-run the compilation, as we have no idea if the compiler
                        //saw the old or new version
                        for (Map.Entry<File, Long> entry : compileTimestamps.entrySet()) {
                            if (entry.getKey().lastModified() != entry.getValue()) {
                                timestampsChanged = true;
                                entry.setValue(entry.getKey().lastModified());
                            }
                        }
                        if (!timestampsChanged) {
                            break;
                        }
                    }
                    //now we re-update the underlying timestamps, to the values we just compiled
                    //if the file has changed in the meantime it will be picked up in the next
                    //scan
                    //note that if compile failed these are not updated, so failing files will always be re-compiled
                    for (Map.Entry<File, Long> entry : compileTimestamps.entrySet()) {
                        sourceFileTimestamps.put(entry.getKey().toPath(), entry.getValue());
                    }
                }

            }

            checkForClassFilesChangesInModule(module, moduleChangedSourceFilePaths, ignoreFirstScanChanges, classScanResult,
                    cuf, timestampSet);

        }

        return classScanResult;
    }

    public Throwable getCompileProblem() {
        return compileProblem;
    }

    private void checkForClassFilesChangesInModule(DevModeContext.ModuleInfo module, List<Path> moduleChangedSourceFiles,
            boolean isInitialRun, ClassScanResult classScanResult,
            Function<DevModeContext.ModuleInfo, DevModeContext.CompilationUnit> cuf, TimestampSet timestampSet) {
        if (cuf.apply(module).getClassesPath() == null) {
            return;
        }

        try {
            for (String folder : cuf.apply(module).getClassesPath().split(File.pathSeparator)) {
                final Path moduleClassesPath = Paths.get(folder);
                if (!Files.exists(moduleClassesPath)) {
                    continue;
                }
                try (final Stream<Path> classesStream = Files.walk(moduleClassesPath)) {
                    final Set<Path> classFilePaths = classesStream
                            .parallel()
                            .filter(path -> path.toString().endsWith(CLASS_EXTENSION))
                            .collect(Collectors.toSet());

                    for (Path classFilePath : classFilePaths) {
                        final Path sourceFilePath = retrieveSourceFilePathForClassFile(classFilePath, moduleChangedSourceFiles,
                                module, cuf, timestampSet);

                        if (sourceFilePath != null) {
                            if (!sourceFilePath.toFile().exists()) {
                                // Source file has been deleted. Delete class and restart
                                cleanUpClassFile(classFilePath, timestampSet);
                                sourceFileTimestamps.remove(sourceFilePath);
                                classScanResult.addDeletedClass(moduleClassesPath, classFilePath);
                            } else {
                                timestampSet.classFilePathToSourceFilePath.put(classFilePath, sourceFilePath);
                                if (classFileWasAdded(classFilePath, isInitialRun, timestampSet)) {
                                    // At least one class was recently modified. Restart.
                                    classScanResult.addAddedClass(moduleClassesPath, classFilePath);
                                } else if (classFileWasRecentModified(classFilePath, isInitialRun, timestampSet)) {
                                    // At least one class was recently modified. Restart.
                                    classScanResult.addChangedClass(moduleClassesPath, classFilePath);
                                } else if (moduleChangedSourceFiles.contains(sourceFilePath)) {
                                    // Source file has been modified, but not the class file
                                    // must be a removed inner class
                                    cleanUpClassFile(classFilePath, timestampSet);
                                    classScanResult.addDeletedClass(moduleClassesPath, classFilePath);
                                }
                            }
                        } else if (classFileWasAdded(classFilePath, isInitialRun, timestampSet)) {
                            classScanResult.addAddedClass(moduleClassesPath, classFilePath);
                        } else if (classFileWasRecentModified(classFilePath, isInitialRun, timestampSet)) {
                            classScanResult.addChangedClass(moduleClassesPath, classFilePath);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path retrieveSourceFilePathForClassFile(Path classFilePath, List<Path> moduleChangedSourceFiles,
            DevModeContext.ModuleInfo module, Function<DevModeContext.ModuleInfo, DevModeContext.CompilationUnit> cuf,
            TimestampSet timestampSet) {
        Path sourceFilePath = timestampSet.classFilePathToSourceFilePath.get(classFilePath);
        if (sourceFilePath == null || moduleChangedSourceFiles.contains(sourceFilePath)) {
            sourceFilePath = compiler.findSourcePath(classFilePath, cuf.apply(module).getSourcePaths(),
                    cuf.apply(module).getClassesPath());
        }
        return sourceFilePath;
    }

    private void cleanUpClassFile(Path classFilePath, TimestampSet timestampSet) throws IOException {
        Files.deleteIfExists(classFilePath);
        timestampSet.classFileChangeTimeStamps.remove(classFilePath);
        timestampSet.classFilePathToSourceFilePath.remove(classFilePath);
    }

    private Optional<String> matchingHandledExtension(Path p) {
        return compiler.allHandledExtensions().stream().filter(e -> p.toString().endsWith(e)).findFirst();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf('.');
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

    Set<String> checkForFileChange() {
        return checkForFileChange(DevModeContext.ModuleInfo::getMain, main);
    }

    Set<String> checkForFileChange(Function<DevModeContext.ModuleInfo, DevModeContext.CompilationUnit> cuf,
            TimestampSet timestampSet) {
        Set<String> ret = new HashSet<>();
        for (DevModeContext.ModuleInfo module : context.getAllModules()) {
            final Set<Path> moduleResources = correspondingResources.computeIfAbsent(cuf.apply(module),
                    m -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
            boolean doCopy = true;
            PathsCollection rootPaths = cuf.apply(module).getResourcePaths();
            String outputPath = cuf.apply(module).getResourcesOutputPath();
            if (rootPaths.isEmpty()) {
                String rootPath = cuf.apply(module).getClassesPath();
                if (rootPath != null) {
                    rootPaths = PathsCollection.of(Paths.get(rootPath));
                }
                outputPath = rootPath;
                doCopy = false;
            }
            if (rootPaths.isEmpty() || outputPath == null) {
                continue;
            }
            final List<Path> roots = rootPaths.toList().stream()
                    .filter(Files::exists)
                    .filter(Files::isReadable)
                    .collect(Collectors.toList());
            //copy all modified non hot deployment files over
            if (doCopy) {
                final Set<Path> seen = new HashSet<>(moduleResources);
                try {
                    for (Path root : roots) {
                        Path outputDir = Paths.get(outputPath);
                        //since the stream is Closeable, use a try with resources so the underlying iterator is closed
                        try (final Stream<Path> walk = Files.walk(root)) {
                            walk.forEach(path -> {
                                try {
                                    Path relative = root.relativize(path);
                                    Path target = outputDir.resolve(relative);
                                    seen.remove(target);
                                    if (!timestampSet.watchedFileTimestamps.containsKey(path)) {
                                        moduleResources.add(target);
                                        if (!Files.exists(target) || Files.getLastModifiedTime(target).toMillis() < Files
                                                .getLastModifiedTime(path).toMillis()) {
                                            if (Files.isDirectory(path)) {
                                                Files.createDirectories(target);
                                            } else {
                                                Files.createDirectories(target.getParent());
                                                ret.add(relative.toString());
                                                byte[] data = Files.readAllBytes(path);
                                                try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                                                    out.write(data);
                                                }
                                                if (copyResourceNotification != null) {
                                                    copyResourceNotification.accept(module, relative.toString());
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("Failed to copy resources", e);
                                }
                            });
                        }
                    }
                    for (Path i : seen) {
                        moduleResources.remove(i);
                        if (!Files.isDirectory(i)) {
                            Files.delete(i);
                        }
                    }
                } catch (IOException e) {
                    log.error("Failed to copy resources", e);
                }
            }

            for (Path root : roots) {
                Path outputDir = Paths.get(outputPath);
                for (String path : timestampSet.watchedFilePaths.keySet()) {
                    Path file = root.resolve(path);
                    if (file.toFile().exists()) {
                        try {
                            long value = Files.getLastModifiedTime(file).toMillis();
                            Long existing = timestampSet.watchedFileTimestamps.get(file);
                            //existing can be null when running tests
                            //as there is both normal and test resources, but only one set of watched timestampts
                            if (existing != null && value > existing) {
                                ret.add(path);
                                //a write can be a 'truncate' + 'write'
                                //if the file is empty we may be seeing the middle of a write
                                if (Files.size(file) == 0) {
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        //ignore
                                    }
                                }
                                //re-read, as we may have read the original TS if the middle of
                                //a truncate+write, even if the write had completed by the time
                                //we read the size
                                value = Files.getLastModifiedTime(file).toMillis();

                                log.infof("File change detected: %s", file);
                                if (doCopy && !Files.isDirectory(file)) {
                                    Path target = outputDir.resolve(path);
                                    byte[] data = Files.readAllBytes(file);
                                    try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                                        out.write(data);
                                    }
                                }
                                timestampSet.watchedFileTimestamps.put(file, value);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else {
                        timestampSet.watchedFileTimestamps.put(file, 0L);
                        Path target = outputDir.resolve(path);
                        try {
                            FileUtil.deleteDirectory(target);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            }
        }

        return ret;
    }

    private boolean sourceFileWasRecentModified(final Path sourcePath, boolean ignoreFirstScanChanges, boolean firstScan) {
        return checkIfFileModified(sourcePath, sourceFileTimestamps, ignoreFirstScanChanges, firstScan);
    }

    private boolean classFileWasRecentModified(final Path classFilePath, boolean ignoreFirstScanChanges,
            TimestampSet timestampSet) {
        return checkIfFileModified(classFilePath, timestampSet.classFileChangeTimeStamps, ignoreFirstScanChanges, true);
    }

    private boolean classFileWasAdded(final Path classFilePath, boolean ignoreFirstScanChanges, TimestampSet timestampSet) {
        final Long lastRecordedChange = timestampSet.classFileChangeTimeStamps.get(classFilePath);
        if (lastRecordedChange == null) {
            try {
                timestampSet.classFileChangeTimeStamps.put(classFilePath, Files.getLastModifiedTime(classFilePath).toMillis());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return lastRecordedChange == null && !ignoreFirstScanChanges;
    }

    private boolean checkIfFileModified(Path path, Map<Path, Long> pathModificationTimes, boolean ignoreFirstScanChanges,
            boolean updateTimestamp) {
        try {
            final long lastModificationTime = Files.getLastModifiedTime(path).toMillis();
            final Long lastRecordedChange = pathModificationTimes.get(path);

            if (lastRecordedChange == null) {
                if (updateTimestamp) {
                    pathModificationTimes.put(path, lastModificationTime);
                }
                return !ignoreFirstScanChanges;
            }

            if (lastRecordedChange != lastModificationTime) {
                if (updateTimestamp) {
                    pathModificationTimes.put(path, lastModificationTime);
                }
                return true;
            }

            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public RuntimeUpdatesProcessor setDisableInstrumentationForClassPredicate(
            Predicate<ClassInfo> disableInstrumentationForClassPredicate) {
        this.disableInstrumentationForClassPredicate = disableInstrumentationForClassPredicate;
        return this;
    }

    public RuntimeUpdatesProcessor setDisableInstrumentationForIndexPredicate(
            Predicate<Index> disableInstrumentationForIndexPredicate) {
        this.disableInstrumentationForIndexPredicate = disableInstrumentationForIndexPredicate;
        return this;
    }

    public RuntimeUpdatesProcessor setWatchedFilePaths(Map<String, Boolean> watchedFilePaths, boolean isTest) {
        if (isTest) {
            setWatchedFilePathsInternal(watchedFilePaths, test, s -> Arrays.asList(s.getTest().get(), s.getMain()));
        } else {
            main.watchedFileTimestamps.clear();
            setWatchedFilePathsInternal(watchedFilePaths, main, s -> Collections.singletonList(s.getMain()));
        }
        return this;
    }

    private RuntimeUpdatesProcessor setWatchedFilePathsInternal(Map<String, Boolean> watchedFilePaths,
            TimestampSet timestamps, Function<DevModeContext.ModuleInfo, List<DevModeContext.CompilationUnit>> cuf) {
        timestamps.watchedFilePaths = watchedFilePaths;
        Map<String, Boolean> extraWatchedFilePaths = new HashMap<>();
        for (DevModeContext.ModuleInfo module : context.getAllModules()) {
            List<DevModeContext.CompilationUnit> compilationUnits = cuf.apply(module);
            for (DevModeContext.CompilationUnit unit : compilationUnits) {
                PathsCollection rootPaths = unit.getResourcePaths();
                if (rootPaths.isEmpty()) {
                    String rootPath = unit.getClassesPath();
                    if (rootPath == null) {
                        continue;
                    }
                    rootPaths = PathsCollection.of(Paths.get(rootPath));
                }
                for (Path root : rootPaths) {
                    for (String path : watchedFilePaths.keySet()) {
                        Path config = root.resolve(path);
                        if (config.toFile().exists()) {
                            try {
                                FileTime lastModifiedTime = Files.getLastModifiedTime(config);
                                timestamps.watchedFileTimestamps.put(config, lastModifiedTime.toMillis());
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        } else {
                            timestamps.watchedFileTimestamps.put(config, 0L);
                            Map<Path, Long> extraWatchedFileTimestamps = expandGlobPattern(root, config);
                            timestamps.watchedFileTimestamps.putAll(extraWatchedFileTimestamps);
                            for (Path extraPath : extraWatchedFileTimestamps.keySet()) {
                                extraWatchedFilePaths.put(root.relativize(extraPath).toString(),
                                        timestamps.watchedFilePaths.get(path));
                            }
                            timestamps.watchedFileTimestamps.putAll(extraWatchedFileTimestamps);
                        }
                    }
                }
            }
        }
        timestamps.watchedFilePaths.putAll(extraWatchedFilePaths);
        return this;
    }

    public void addHotReplacementSetup(HotReplacementSetup service) {
        hotReplacementSetup.add(service);
    }

    public void startupFailed() {
        for (HotReplacementSetup i : hotReplacementSetup) {
            i.handleFailedInitialStart();
        }
        //if startup failed we always do a class loader based restart
        lastStartIndex = null;
    }

    public static void setLastStartIndex(IndexView lastStartIndex) {
        RuntimeUpdatesProcessor.lastStartIndex = lastStartIndex;
    }

    @Override
    public void close() throws IOException {
        compiler.close();
        if (testClassChangeWatcher != null) {
            testClassChangeWatcher.close();
        }
        if (testClassChangeTimer != null) {
            testClassChangeTimer.cancel();
        }
    }

    private Map<Path, Long> expandGlobPattern(Path root, Path configFile) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + configFile.toString());
        Map<Path, Long> files = new HashMap<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (pathMatcher.matches(file)) {
                        files.put(file, attrs.lastModifiedTime().toMillis());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    public boolean toggleInstrumentation() {
        instrumentationEnabled = !instrumentationEnabled();
        if (instrumentationEnabled) {
            log.info("Instrumentation based restart enabled");
        } else {
            log.info("Instrumentation based restart disabled");
        }
        return instrumentationEnabled;
    }

    public boolean toggleLiveReloadEnabled() {
        liveReloadEnabled = !liveReloadEnabled;
        if (liveReloadEnabled) {
            log.info("Live reload enabled");
        } else {
            log.info("Live reload disabled");
        }
        return liveReloadEnabled;
    }

    public boolean isLiveReloadEnabled() {
        return liveReloadEnabled;
    }

    static class TimestampSet {
        final Map<Path, Long> watchedFileTimestamps = new ConcurrentHashMap<>();
        final Map<Path, Long> classFileChangeTimeStamps = new ConcurrentHashMap<>();
        final Map<Path, Path> classFilePathToSourceFilePath = new ConcurrentHashMap<>();
        // file path -> isRestartNeeded
        private volatile Map<String, Boolean> watchedFilePaths = Collections.emptyMap();

        public void merge(TimestampSet other) {
            watchedFileTimestamps.putAll(other.watchedFileTimestamps);
            classFileChangeTimeStamps.putAll(other.classFileChangeTimeStamps);
            classFilePathToSourceFilePath.putAll(other.classFilePathToSourceFilePath);
            Map<String, Boolean> newVal = new HashMap<>(watchedFilePaths);
            newVal.putAll(other.watchedFilePaths);
            watchedFilePaths = newVal;

        }
    }

}
