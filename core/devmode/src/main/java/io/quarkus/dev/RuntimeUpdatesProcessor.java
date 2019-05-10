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
import java.nio.file.Paths;
import java.util.ArrayList;
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

    private final DevModeContext context;
    private final ClassLoaderCompiler compiler;
    private volatile long lastChange = System.currentTimeMillis();

    private volatile Set<String> configFilePaths = Collections.emptySet();
    private final Map<Path, Long> configFileTimestamps = new ConcurrentHashMap<>();

    private static final Logger log = Logger.getLogger(RuntimeUpdatesProcessor.class.getPackage().getName());
    private final List<Runnable> preScanSteps = new CopyOnWriteArrayList<>();

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
    public Path getSourcesDir() {
        //TODO: fix all these
        for (DevModeContext.ModuleInfo i : context.getModules()) {
            if (i.getSourcePath() != null) {
                return Paths.get(i.getSourcePath());
            }
        }
        return null;
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
        return DevModeMain.deploymentProblem;
    }

    @Override
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

        for (DevModeContext.ModuleInfo i : context.getModules()) {
            if (i.getSourcePath() != null) {
                final Set<File> changedSourceFiles;
                try (final Stream<Path> sourcesStream = Files.walk(Paths.get(i.getSourcePath()))) {
                    changedSourceFiles = sourcesStream
                            .parallel()
                            .filter(p -> matchingHandledExtension(p).isPresent())
                            .filter(p -> wasRecentlyModified(p, i))
                            .map(Path::toFile)
                            //Needing a concurrent Set, not many standard options:
                            .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
                }
                if (!changedSourceFiles.isEmpty()) {
                    log.info("Changed source files detected, recompiling " + changedSourceFiles);
                    try {
                        compiler.compile(i.getSourcePath(), changedSourceFiles.stream()
                                .collect(groupingBy(this::getFileExtension, Collectors.toSet())));
                    } catch (Exception e) {
                        DevModeMain.deploymentProblem = e;
                        return false;
                    }
                }
            }
        }

        for (DevModeContext.ModuleInfo i : context.getModules()) {
            if (i.getClassesPath() != null) {
                try (final Stream<Path> classesStream = Files.walk(Paths.get(i.getClassesPath()))) {
                    if (classesStream.parallel().anyMatch(p -> p.toString().endsWith(".class") && wasRecentlyModified(p, i))) {
                        // At least one class was recently modified
                        lastChange = System.currentTimeMillis();
                        return true;
                    }
                }
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
        boolean configFilesHaveChanged = false;
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

            for (String configFilePath : configFilePaths) {
                Path config = root.resolve(configFilePath);
                if (Files.exists(config)) {
                    try {
                        long value = Files.getLastModifiedTime(config).toMillis();
                        Long existing = configFileTimestamps.get(config);
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
                            configFileTimestamps.put(config, value);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    configFileTimestamps.put(config, 0L);
                    Path target = classesDir.resolve(configFilePath);
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return configFilesHaveChanged;
    }

    private boolean wasRecentlyModified(final Path p, DevModeContext.ModuleInfo module) {
        try {
            long sourceMod = Files.getLastModifiedTime(p).toMillis();
            boolean recent = sourceMod > lastChange;
            if (recent) {
                return true;
            }
            if (module.getSourcePath() == null || module.getClassesPath() == null) {
                return false;
            }
            Path sourcesDir = Paths.get(module.getSourcePath());
            Path classesDir = Paths.get(module.getClassesPath());

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

        for (DevModeContext.ModuleInfo module : context.getModules()) {
            String rootPath = module.getResourcePath();

            if (rootPath == null) {
                rootPath = module.getClassesPath();
            }
            if (rootPath == null) {
                continue;
            }
            Path root = Paths.get(rootPath);
            for (String i : configFilePaths) {
                Path config = root.resolve(i);
                if (Files.exists(config)) {
                    try {
                        configFileTimestamps.put(config, Files.getLastModifiedTime(config).toMillis());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    configFileTimestamps.put(config, 0L);
                }
            }
        }
        return this;
    }

}
