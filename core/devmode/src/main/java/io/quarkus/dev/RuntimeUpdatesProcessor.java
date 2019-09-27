package io.quarkus.dev;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.runtime.Timing;

public class RuntimeUpdatesProcessor implements HotReplacementContext {
    private static final String CLASS_EXTENSION = ".class";
    private static final Logger log = Logger.getLogger(RuntimeUpdatesProcessor.class.getPackage().getName());

    private final DevModeContext context;
    private final ClassLoaderCompiler compiler;

    // file path -> isRestartNeeded
    private volatile Map<String, Boolean> watchedFilePaths = Collections.emptyMap();

    /**
     * A first scan is considered done when we have visited all modules at least once.
     * This is useful in two ways.
     * - To make sure that source time stamps have been recorded at least once
     * - To avoid re-compiling on first run by ignoring all first time changes detected by
     * {@link RuntimeUpdatesProcessor#checkIfFileModified(Path, Map, boolean)} during the first scan.
     */
    private volatile boolean firstScanDone = false;

    private final Map<Path, Long> sourceFileTimestamps = new ConcurrentHashMap<>();
    private final Map<Path, Long> watchedFileTimestamps = new ConcurrentHashMap<>();
    private final Map<Path, Long> classFileChangeTimeStamps = new ConcurrentHashMap<>();
    private final Map<Path, Path> classFilePathToSourceFilePath = new ConcurrentHashMap<>();

    /**
     * Resources that appear in both src and target, these will be removed if the src resource subsequently disappears.
     * This set contains the paths in the target dir
     */
    private final Set<Path> correspondingResources = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Runnable> preScanSteps = new CopyOnWriteArrayList<>();
    private final List<Consumer<Set<String>>> noRestartChangesConsumers = new CopyOnWriteArrayList<>();
    private final List<HotReplacementSetup> hotReplacementSetup = new ArrayList<>();
    private final DevModeMain devModeMain;

    public RuntimeUpdatesProcessor(DevModeContext context, ClassLoaderCompiler compiler, DevModeMain devModeMain) {
        this.context = context;
        this.compiler = compiler;
        this.devModeMain = devModeMain;
    }

    @Override
    public Path getClassesDir() {
        //TODO: fix all these
        for (DevModeContext.ModuleInfo i : context.getModules()) {
            return Paths.get(i.getResourcePath());
        }
        return null;
    }

    @Override
    public List<Path> getSourcesDir() {
        return context.getModules().stream().flatMap(m -> m.getSourcePaths().stream()).map(Paths::get).collect(toList());
    }

    @Override
    public List<Path> getResourcesDir() {
        List<Path> ret = new ArrayList<>();
        for (DevModeContext.ModuleInfo i : context.getModules()) {
            if (i.getResourcePath() != null) {
                ret.add(Paths.get(i.getResourcePath()));
            }
        }
        Collections.reverse(ret); //make sure the actual project is before dependencies
        return ret;
    }

    @Override
    public Throwable getDeploymentProblem() {
        //we differentiate between these internally, however for the error reporting they are the same
        return DevModeMain.compileProblem != null ? DevModeMain.compileProblem : DevModeMain.deploymentProblem;
    }

    @Override
    public boolean isTest() {
        return context.isTest();
    }

    @Override
    public boolean doScan(boolean userInitiated) throws IOException {
        final long startNanoseconds = System.nanoTime();
        for (Runnable step : preScanSteps) {
            try {
                step.run();
            } catch (Throwable t) {
                log.error("Pre Scan step failed", t);
            }
        }

        boolean classChanged = checkForChangedClasses();
        Set<String> filesChanged = checkForFileChange();

        //if there is a deployment problem we always restart on scan
        //this is because we can't setup the config file watches
        //in an ideal world we would just check every resource file for changes, however as everything is already
        //all broken we just assume the reason that they have refreshed is because they have fixed something
        //trying to watch all resource files is complex and this is likely a good enough solution for what is already an edge case
        boolean restartNeeded = classChanged || (DevModeMain.deploymentProblem != null && userInitiated);
        if (!restartNeeded && !filesChanged.isEmpty()) {
            restartNeeded = filesChanged.stream().map(watchedFilePaths::get).anyMatch(Boolean.TRUE::equals);
        }
        if (restartNeeded) {
            devModeMain.restartApp(filesChanged);
            log.infof("Hot replace total time: %ss ", Timing.convertToBigDecimalSeconds(System.nanoTime() - startNanoseconds));
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
        }
        return false;
    }

    @Override
    public void addPreScanStep(Runnable runnable) {
        preScanSteps.add(runnable);
    }

    @Override
    public void consumeNoRestartChanges(Consumer<Set<String>> consumer) {
        noRestartChangesConsumers.add(consumer);
    }

    boolean checkForChangedClasses() throws IOException {
        boolean hasChanges = false;
        boolean ignoreFirstScanChanges = !firstScanDone;

        for (DevModeContext.ModuleInfo module : context.getModules()) {
            final List<Path> moduleChangedSourceFilePaths = new ArrayList<>();

            for (String sourcePath : module.getSourcePaths()) {
                final Set<File> changedSourceFiles;
                try (final Stream<Path> sourcesStream = Files.walk(Paths.get(sourcePath))) {
                    changedSourceFiles = sourcesStream
                            .parallel()
                            .filter(p -> matchingHandledExtension(p).isPresent()
                                    && sourceFileWasRecentModified(p, ignoreFirstScanChanges))
                            .map(Path::toFile)
                            //Needing a concurrent Set, not many standard options:
                            .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
                }
                if (!changedSourceFiles.isEmpty()) {
                    log.info("Changed source files detected, recompiling " + changedSourceFiles);
                    try {
                        final Set<Path> changedPaths = changedSourceFiles.stream()
                                .map(File::toPath)
                                .collect(Collectors.toSet());
                        moduleChangedSourceFilePaths.addAll(changedPaths);
                        compiler.compile(sourcePath, changedSourceFiles.stream()
                                .collect(groupingBy(this::getFileExtension, Collectors.toSet())));
                        DevModeMain.compileProblem = null;
                    } catch (Exception e) {
                        DevModeMain.compileProblem = e;
                        return false;
                    }
                }

            }

            if (checkForClassFilesChangesInModule(module, moduleChangedSourceFilePaths, ignoreFirstScanChanges)) {
                hasChanges = true;
            }
        }

        this.firstScanDone = true;
        return hasChanges;
    }

    private boolean checkForClassFilesChangesInModule(DevModeContext.ModuleInfo module, List<Path> moduleChangedSourceFiles,
            boolean isInitialRun) {
        boolean hasChanges = !moduleChangedSourceFiles.isEmpty();

        if (module.getClassesPath() == null) {
            return hasChanges;
        }

        try {
            for (String folder : module.getClassesPath().split(File.pathSeparator)) {
                final Path moduleClassesPath = Paths.get(folder);
                try (final Stream<Path> classesStream = Files.walk(moduleClassesPath)) {
                    final Set<Path> classFilePaths = classesStream
                            .parallel()
                            .filter(path -> path.toString().endsWith(CLASS_EXTENSION))
                            .collect(Collectors.toSet());

                    for (Path classFilePath : classFilePaths) {
                        final Path sourceFilePath = retrieveSourceFilePathForClassFile(classFilePath, moduleChangedSourceFiles,
                                module);

                        if (sourceFilePath != null) {
                            if (!sourceFilePath.toFile().exists()) {
                                // Source file has been deleted. Delete class and restart
                                cleanUpClassFile(classFilePath);
                                sourceFileTimestamps.remove(sourceFilePath);
                                hasChanges = true;
                            } else {
                                classFilePathToSourceFilePath.put(classFilePath, sourceFilePath);
                                if (classFileWasRecentModified(classFilePath, isInitialRun)) {
                                    // At least one class was recently modified. Restart.
                                    hasChanges = true;
                                } else if (moduleChangedSourceFiles.contains(sourceFilePath)) {
                                    // Source file has been modified, we delete the .class files as they are going to
                                    // be recompiled anyway, this allows for simple cleanup of inner classes
                                    cleanUpClassFile(classFilePath);
                                    hasChanges = true;
                                }
                            }
                        } else if (classFileWasRecentModified(classFilePath, isInitialRun)) {
                            hasChanges = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return hasChanges;
    }

    private Path retrieveSourceFilePathForClassFile(Path classFilePath, List<Path> moduleChangedSourceFiles,
            DevModeContext.ModuleInfo module) {
        Path sourceFilePath = classFilePathToSourceFilePath.get(classFilePath);
        if (sourceFilePath == null || moduleChangedSourceFiles.contains(sourceFilePath)) {
            sourceFilePath = compiler.findSourcePath(classFilePath, module.getSourcePaths(), module.getClassesPath());
        }
        return sourceFilePath;
    }

    private void cleanUpClassFile(Path classFilePath) throws IOException {
        Files.deleteIfExists(classFilePath);
        classFileChangeTimeStamps.remove(classFilePath);
        classFilePathToSourceFilePath.remove(classFilePath);
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

    private Set<String> checkForFileChange() {
        Set<String> ret = new HashSet<>();
        for (DevModeContext.ModuleInfo module : context.getModules()) {
            boolean doCopy = true;
            String rootPath = module.getResourcePath();
            if (rootPath == null) {
                rootPath = module.getClassesPath();
                doCopy = false;
            }
            if (rootPath == null) {
                continue;
            }
            Path root = Paths.get(rootPath);
            Path classesDir = Paths.get(module.getClassesPath());
            //copy all modified non hot deployment files over
            if (doCopy) {
                try {
                    Set<Path> seen = new HashSet<>(correspondingResources);
                    Files.walk(root).forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            try {
                                Path relative = root.relativize(path);
                                Path target = classesDir.resolve(relative);
                                seen.remove(target);
                                if (!watchedFileTimestamps.containsKey(path)) {
                                    correspondingResources.add(target);
                                    if (!Files.exists(target) || Files.getLastModifiedTime(target).toMillis() < Files
                                            .getLastModifiedTime(path).toMillis()) {
                                        if (Files.isDirectory(path)) {
                                            Files.createDirectories(target);
                                        } else {
                                            Files.createDirectories(target.getParent());
                                            byte[] data = Files.readAllBytes(path);
                                            try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                                                out.write(data);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Failed to copy resources", e);
                            }
                        }
                    });
                    for (Path i : seen) {
                        if (!Files.isDirectory(i)) {
                            Files.delete(i);
                        }
                    }
                } catch (IOException e) {
                    log.error("Failed to copy resources", e);
                }

            }

            for (String path : watchedFilePaths.keySet()) {
                Path file = root.resolve(path);
                if (file.toFile().exists()) {
                    try {
                        long value = Files.getLastModifiedTime(file).toMillis();
                        Long existing = watchedFileTimestamps.get(file);
                        if (value > existing) {
                            ret.add(path);
                            log.infof("File change detected: %s", file);
                            if (doCopy) {
                                Path target = classesDir.resolve(path);
                                byte[] data = Files.readAllBytes(file);
                                try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                                    out.write(data);
                                }
                            }
                            watchedFileTimestamps.put(file, value);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    watchedFileTimestamps.put(file, 0L);
                    Path target = classesDir.resolve(path);
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return ret;
    }

    private boolean sourceFileWasRecentModified(final Path sourcePath, boolean ignoreFirstScanChanges) {
        return checkIfFileModified(sourcePath, sourceFileTimestamps, ignoreFirstScanChanges);
    }

    private boolean classFileWasRecentModified(final Path classFilePath, boolean ignoreFirstScanChanges) {
        return checkIfFileModified(classFilePath, classFileChangeTimeStamps, ignoreFirstScanChanges);
    }

    private boolean checkIfFileModified(Path path, Map<Path, Long> pathModificationTimes, boolean ignoreFirstScanChanges) {
        try {
            final long lastModificationTime = Files.getLastModifiedTime(path).toMillis();
            final Long lastRecordedChange = pathModificationTimes.get(path);

            if (lastRecordedChange == null) {
                pathModificationTimes.put(path, lastModificationTime);
                return !ignoreFirstScanChanges;
            }

            if (lastRecordedChange != lastModificationTime) {
                pathModificationTimes.put(path, lastModificationTime);
                return true;
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeUpdatesProcessor setWatchedFilePaths(Map<String, Boolean> watchedFilePaths) {
        this.watchedFilePaths = watchedFilePaths;
        watchedFileTimestamps.clear();

        for (DevModeContext.ModuleInfo module : context.getModules()) {
            String rootPath = module.getResourcePath();

            if (rootPath == null) {
                rootPath = module.getClassesPath();
            }
            if (rootPath == null) {
                continue;
            }
            Path root = Paths.get(rootPath);
            for (String path : watchedFilePaths.keySet()) {
                Path config = root.resolve(path);
                if (config.toFile().exists()) {
                    try {
                        watchedFileTimestamps.put(config, Files.getLastModifiedTime(config).toMillis());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    watchedFileTimestamps.put(config, 0L);
                }
            }
        }
        return this;
    }

    public void addHotReplacementSetup(HotReplacementSetup service) {
        hotReplacementSetup.add(service);
    }

    public void startupFailed() {
        for (HotReplacementSetup i : hotReplacementSetup) {
            i.handleFailedInitialStart();
        }
    }

}
