/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.runtime.Timing;

public class RuntimeUpdatesProcessor implements HotReplacementContext {

    private final DevModeContext context;
    private final ClassLoaderCompiler compiler;
    private volatile long lastChange = System.currentTimeMillis();

    private volatile Set<String> watchedFilePaths = Collections.emptySet();
    private final Map<Path, Long> watchedFileTimestamps = new ConcurrentHashMap<>();

    private static final Logger log = Logger.getLogger(RuntimeUpdatesProcessor.class.getPackage().getName());
    private final List<Runnable> preScanSteps = new CopyOnWriteArrayList<>();
    private final List<HotReplacementSetup> hotReplacementSetup = new ArrayList<>();

    public RuntimeUpdatesProcessor(DevModeContext context, ClassLoaderCompiler compiler) {
        this.context = context;
        this.compiler = compiler;
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
    public boolean doScan(boolean userInitiated) throws IOException {
        final long startNanoseconds = System.nanoTime();
        for (Runnable i : preScanSteps) {
            try {
                i.run();
            } catch (Throwable t) {
                log.error("Pre Scan step failed", t);
            }
        }

        boolean classChanged = checkForChangedClasses();
        Set<String> configFileChanged = checkForFileChange();

        //if there is a deployment problem we always restart on scan
        //this is because we can't setup the config file watches
        //in an ideal world we would just check every resource file for changes, however as everything is already
        //all broken we just assume the reason that they have refreshed is because they have fixed something
        //trying to watch all resource files is complex and this is likely a good enough solution for what is already an edge case
        if (classChanged || !configFileChanged.isEmpty() || (DevModeMain.deploymentProblem != null && userInitiated)) {
            DevModeMain.restartApp(configFileChanged);
            log.infof("Hot replace total time: %ss ", Timing.convertToBigDecimalSeconds(System.nanoTime() - startNanoseconds));
            return true;
        }
        return false;
    }

    @Override
    public void addPreScanStep(Runnable runnable) {
        preScanSteps.add(runnable);
    }

    boolean checkForChangedClasses() throws IOException {
        boolean hasChanges = false;

        for (DevModeContext.ModuleInfo i : context.getModules()) {
            for (String sourcePath : i.getSourcePaths()) {
                final Set<File> changedSourceFiles;
                try (final Stream<Path> sourcesStream = Files.walk(Paths.get(sourcePath))) {
                    changedSourceFiles = sourcesStream
                            .parallel()
                            .filter(p -> matchingHandledExtension(p).isPresent())
                            .filter(p -> wasRecentlyModified(p))
                            .map(Path::toFile)
                            //Needing a concurrent Set, not many standard options:
                            .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
                }
                if (!changedSourceFiles.isEmpty()) {
                    log.info("Changed source files detected, recompiling " + changedSourceFiles);
                    try {
                        compiler.compile(sourcePath, changedSourceFiles.stream()
                                .collect(groupingBy(this::getFileExtension, Collectors.toSet())));
                        hasChanges = true;
                        DevModeMain.compileProblem = null;
                    } catch (Exception e) {
                        DevModeMain.compileProblem = e;
                        return false;
                    }
                }
            }
        }

        if (hasChanges) {
            lastChange = System.currentTimeMillis();
        }

        return hasChanges;
    }

    private Optional<String> matchingHandledExtension(Path p) {
        return compiler.allHandledExtensions().stream().filter(e -> p.toString().endsWith(e)).findFirst();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
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

            for (String path : watchedFilePaths) {
                Path file = root.resolve(path);
                if (Files.exists(file)) {
                    try {
                        long value = Files.getLastModifiedTime(file).toMillis();
                        Long existing = watchedFileTimestamps.get(file);
                        if (value > existing) {
                            ret.add(path);
                            log.infof("File change detected: %s", file);
                            if (doCopy) {
                                Path target = classesDir.resolve(path);
                                byte[] data = CopyUtils.readFileContent(file);
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

    private boolean wasRecentlyModified(final Path p) {
        try {
            long sourceMod = Files.getLastModifiedTime(p).toMillis();
            return sourceMod > lastChange;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeUpdatesProcessor setWatchedFilePaths(Set<String> watchedFilePaths) {
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
            for (String i : watchedFilePaths) {
                Path config = root.resolve(i);
                if (Files.exists(config)) {
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
