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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import io.quarkus.deployment.devmode.HotReplacementContext;

public class RuntimeUpdatesProcessor implements HotReplacementContext {

    private final Path classesDir;
    private final Path sourcesDir;
    private final Path resourcesDir;
    private final ClassLoaderCompiler compiler;
    private volatile long lastChange = System.currentTimeMillis();

    private volatile Set<String> configFilePaths = Collections.emptySet();
    private final Map<String, Long> configFileTimestamps = new ConcurrentHashMap<>();

    private static final Logger log = Logger.getLogger(RuntimeUpdatesProcessor.class.getPackage().getName());

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

    public void doScan() throws IOException {
        final long start = System.currentTimeMillis();
        final ConcurrentMap<String, byte[]> changedClasses = scanForChangedClasses();
        if (changedClasses == null) return;

        DevModeMain.restartApp();
        log.info("Hot replace total time: " + (System.currentTimeMillis() - start) + "ms");
    }

    ConcurrentMap<String, byte[]> scanForChangedClasses() throws IOException {
        final Set<File> changedSourceFiles;

        if (sourcesDir != null) {
            try (final Stream<Path> sourcesStream = Files.walk(sourcesDir)) {
                changedSourceFiles = sourcesStream
                        .parallel()
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> wasRecentlyModified(p))
                        .map(Path::toFile)
                        //Needing a concurrent Set, not many standard options:
                        .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
            }
        } else {
            changedSourceFiles = Collections.EMPTY_SET;
        }
        if (!changedSourceFiles.isEmpty()) {
            log.info("Changes source files detected, recompiling " + changedSourceFiles);

            try {
                compiler.compile(changedSourceFiles);
            } catch (Exception e) {
                DevModeMain.deploymentProblem = e;
                return null;
            }
        }
        final ConcurrentMap<String, byte[]> changedClasses;
        try (final Stream<Path> classesStream = Files.walk(classesDir)) {
            changedClasses = classesStream
                    .parallel()
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> wasRecentlyModified(p))
                    .collect(Collectors.toConcurrentMap(
                            p -> pathToClassName(p),
                            p -> CopyUtils.readFileContentNoIOExceptions(p))
                    );
        }
        if (changedClasses.isEmpty() && !checkForConfigFileChange()) {
            return null;
        }

        lastChange = System.currentTimeMillis();
        return changedClasses;
    }

    private boolean checkForConfigFileChange() {
        boolean ret = false;
        boolean doCopy = true;
        Path root = resourcesDir;
        if (root == null) {
            root = classesDir;
            doCopy = false;
        }
        for (String i : configFilePaths) {
            Path config = root.resolve(i);
            if (Files.exists(config)) {
                try {
                    long value = Files.getLastModifiedTime(config).toMillis();
                    Long existing = configFileTimestamps.get(i);
                    if (value > existing) {
                        ret = true;
                        if (doCopy) {
                            Path target = classesDir.resolve(i);
                            byte[] data = CopyUtils.readFileContent(config);
                            try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                                out.write(data);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return ret;
    }

    private boolean wasRecentlyModified(final Path p) {
        try {
            long sourceMod = Files.getLastModifiedTime(p).toMillis();
            boolean recent = sourceMod > lastChange;
            if (recent) {
                return true;
            }
            if (p.toString().endsWith(".java")) {
                String pathName = sourcesDir.relativize(p).toString();
                String classFileName = pathName.substring(0, pathName.length() - 5) + ".class";
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

    private String pathToClassName(final Path path) {
        String pathName = classesDir.relativize(path).toString();
        String className = pathName.substring(0, pathName.length() - 6).replace('/', '.');
        return className;
    }

    interface UpdateHandler {

        void handle(Map<String, byte[]> changed);

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
