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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
import io.quarkus.runtime.Timing;

public class RuntimeUpdatesProcessor implements HotReplacementContext {

    private final Path classesDir;
    private final Path sourcesDir;
    private final Path resourcesDir;
    private final ClassLoaderCompiler compiler;
    private volatile long lastChange = System.currentTimeMillis();

    private volatile Set<String> configFilePaths = Collections.emptySet();
    private final Map<String, Long> configFileTimestamps = new ConcurrentHashMap<>();

    private static final Logger log = Logger.getLogger(RuntimeUpdatesProcessor.class.getPackage().getName());
    private final List<Runnable> preScanSteps = new CopyOnWriteArrayList<>();

    public RuntimeUpdatesProcessor(Path classesDir, Path sourcesDir, Path resourcesDir, ClassLoaderCompiler compiler) {
        this.classesDir = classesDir;
        this.sourcesDir = sourcesDir;
        this.resourcesDir = resourcesDir;
        this.compiler = compiler;
    }

    @Override
    public Path getClassesDir() {
        return classesDir;
    }

    @Override
    public Path getSourcesDir() {
        return sourcesDir;
    }

    @Override
    public Path getResourcesDir() {
        return resourcesDir;
    }

    @Override
    public Throwable getDeploymentProblem() {
        return DevModeMain.deploymentProblem;
    }

    public boolean doScan() throws IOException {
        final long startNanoseconds = System.nanoTime();
        for (Runnable i : preScanSteps) {
            try {
                i.run();
            } catch (Throwable t) {
                log.error("Pre Scan step failed", t);
            }
        }

        boolean classChanged = checkForChangedClasses();
        boolean configFileChanged = checkForConfigFileChange();

        if (classChanged || configFileChanged) {
            DevModeMain.restartApp();
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
        final Set<File> changedSourceFiles;

        if (sourcesDir != null) {
            try (final Stream<Path> sourcesStream = Files.walk(sourcesDir)) {
                changedSourceFiles = sourcesStream
                        .parallel()
                        .filter(p -> matchingHandledExtension(p).isPresent())
                        .filter(p -> wasRecentlyModified(p))
                        .map(Path::toFile)
                        //Needing a concurrent Set, not many standard options:
                        .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
            }
        } else {
            changedSourceFiles = Collections.emptySet();
        }
        if (!changedSourceFiles.isEmpty()) {
            log.info("Changed source files detected, recompiling " + changedSourceFiles);
            try {
                compiler.compile(changedSourceFiles.stream()
                        .collect(groupingBy(this::getFileExtension, Collectors.toSet())));
            } catch (Exception e) {
                DevModeMain.deploymentProblem = e;
                return false;
            }
        }
        try (final Stream<Path> classesStream = Files.walk(classesDir)) {
            if (classesStream.parallel().anyMatch(p -> p.toString().endsWith(".class") && wasRecentlyModified(p))) {
                // At least one class was recently modified
                lastChange = System.currentTimeMillis();
                return true;
            }
        }
        return false;
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

    private boolean checkForConfigFileChange() {
        Path root = resourcesDir;
        boolean doCopy = true;
        boolean configFilesHaveChanged = false;

        if (root == null) {
            root = classesDir;
            doCopy = false;
        }

        for (String configFilePath : configFilePaths) {
            Path config = root.resolve(configFilePath);
            if (Files.exists(config)) {
                try {
                    long value = Files.getLastModifiedTime(config).toMillis();
                    Long existing = configFileTimestamps.get(configFilePath);
                    if (value > existing) {
                        configFilesHaveChanged = true;
                        log.infof("Config file change detected: %s", config);
                        if (doCopy) {
                            Path target = classesDir.resolve(configFilePath);
                            byte[] data = CopyUtils.readFileContent(config);
                            try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                                out.write(data);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                configFileTimestamps.remove(configFilePath);
                Path target = classesDir.resolve(configFilePath);
                try {
                    Files.deleteIfExists(target);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return configFilesHaveChanged;
    }

    private boolean wasRecentlyModified(final Path p) {
        try {
            long sourceMod = Files.getLastModifiedTime(p).toMillis();
            boolean recent = sourceMod > lastChange;
            if (recent) {
                return true;
            }
            Optional<String> matchingExtension = matchingHandledExtension(p);
            if (matchingExtension.isPresent()) {
                String pathName = sourcesDir.relativize(p).toString();
                String classFileName = pathName.substring(0, pathName.length() - matchingExtension.get().length()) + ".class";
                Path classFile = classesDir.resolve(classFileName);
                if (!Files.exists(classFile)) {
                    return true;
                }
                return sourceMod > Files.getLastModifiedTime(classFile).toMillis();
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeUpdatesProcessor setConfigFilePaths(Set<String> configFilePaths) {
        this.configFilePaths = configFilePaths;
        configFileTimestamps.clear();
        Path root = resourcesDir;
        if (root == null) {
            root = classesDir;
        }
        for (String i : configFilePaths) {
            Path config = root.resolve(i);
            if (Files.exists(config)) {
                try {
                    configFileTimestamps.put(i, Files.getLastModifiedTime(config).toMillis());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                configFileTimestamps.put(i, 0L);
            }
        }

        return this;
    }

}
