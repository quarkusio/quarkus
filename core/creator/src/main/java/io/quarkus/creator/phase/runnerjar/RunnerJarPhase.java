/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.phase.runnerjar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppArtifactResolver;
import io.quarkus.creator.AppCreationPhase;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;
import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.phase.augment.AugmentOutcome;
import io.quarkus.creator.phase.curate.CurateOutcome;
import io.quarkus.creator.util.IoUtils;
import io.quarkus.creator.util.ZipUtils;

/**
 * Based on the provided {@link io.quarkus.creator.phase.augment.AugmentOutcome},
 * this phase builds a runnable JAR.
 *
 * @author Alexey Loubyansky
 */
public class RunnerJarPhase implements AppCreationPhase<RunnerJarPhase>, RunnerJarOutcome {

    private static final String DEFAULT_MAIN_CLASS = "io.quarkus.runner.GeneratedMain";
    private static final String PROVIDED = "provided";

    private static final Logger log = Logger.getLogger(RunnerJarPhase.class);

    private static final Set<String> IGNORED_ENTRIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "META-INF/INDEX.LIST",
            "META-INF/MANIFEST.MF",
            "module-info.class",
            "META-INF/LICENSE",
            "META-INF/NOTICE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE.txt",
            "dependencies.runtime",
            "META-INF/README",
            "META-INF/quarkus-config-roots.list",
            "META-INF/DEPENDENCIES",
            "META-INF/beans.xml",
            "META-INF/quarkus-javadoc.properties",
            "LICENSE")));

    private Path outputDir;
    private Path libDir;
    private Path runnerJar;

    private String finalName;

    private String mainClass = DEFAULT_MAIN_CLASS;

    private boolean uberJar;

    /**
     * Output directory for the outcome of this phase.
     * If not set by the user the work directory of the creator
     * will be used instead.
     *
     * @param outputDir output directory for this phase
     * @return this phase instance
     */
    public RunnerJarPhase setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /**
     * Directory for application dependencies. If none set by the user
     * lib directory will be created in the output directory of the phase.
     *
     * @param libDir directory for project dependencies
     * @return this phase instance
     */
    public RunnerJarPhase setLibDir(Path libDir) {
        this.libDir = libDir;
        return this;
    }

    /**
     * Name for the runnable JAR. If none is provided by the user
     * the name will derived from the user application JAR filename.
     *
     * @param finalName runnable JAR name
     * @return this phase instance
     */
    public RunnerJarPhase setFinalName(String finalName) {
        this.finalName = finalName;
        return this;
    }

    /**
     * Main class name fir the runnable JAR. If none is set by the user
     * io.quarkus.runner.GeneratedMain will be use by default.
     *
     * @param mainClass main class name for the runnable JAR
     * @return
     */
    public RunnerJarPhase setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Whether to build an uber JAR. The default is false.
     *
     * @param uberJar whether to build an uber JAR
     * @return this phase instance
     */
    public RunnerJarPhase setUberJar(boolean uberJar) {
        this.uberJar = uberJar;
        return this;
    }

    @Override
    public Path getRunnerJar() {
        return runnerJar;
    }

    @Override
    public Path getLibDir() {
        return libDir;
    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(RunnerJarOutcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {
        final CurateOutcome appState = ctx.resolveOutcome(CurateOutcome.class);

        outputDir = outputDir == null ? ctx.getWorkPath() : IoUtils.mkdirs(outputDir);

        libDir = IoUtils.mkdirs(libDir == null ? outputDir.resolve("lib") : libDir);

        if (finalName == null) {
            final String name = appState.getArtifactResolver().resolve(appState.getAppArtifact()).getFileName().toString();
            int i = name.lastIndexOf('.');
            if (i > 0) {
                finalName = name.substring(0, i);
            }
        }

        runnerJar = outputDir.resolve(finalName + "-runner.jar");
        IoUtils.recursiveDelete(runnerJar);
        try (FileSystem zipFs = ZipUtils.newZip(runnerJar)) {
            buildRunner(zipFs, appState, ctx.resolveOutcome(AugmentOutcome.class));
        } catch (Exception e) {
            throw new AppCreatorException("Failed to build a runner jar", e);
        }

        try {
            runnerJar.toFile().setReadable(true, false);
        } catch (Exception e) {
            log.warn("Unable to set proper permissions on " + runnerJar);
        }

        // when using uberJar, we rename the standard jar to include the .original suffix
        // this greatly aids tools (such as s2i) that look for a single jar in the output directory to work OOTB
        if (uberJar) {
            try {
                Path originalFile = outputDir.resolve(finalName + ".jar.original");
                if (Files.exists(originalFile)) {
                    Files.delete(originalFile);
                }
                Files.move(outputDir.resolve(finalName + ".jar"), originalFile);
            } catch (IOException e) {
                throw new AppCreatorException("Unable to build uberjar", e);
            }
        }

        ctx.pushOutcome(RunnerJarOutcome.class, this);
    }

    private void buildRunner(FileSystem runnerZipFs, CurateOutcome appState, AugmentOutcome augmentOutcome) throws Exception {

        log.info("Building jar: " + runnerJar);

        final AppArtifactResolver depResolver = appState.getArtifactResolver();
        final List<AppDependency> appDeps = appState.getEffectiveDeps();
        final Map<String, String> seen = new HashMap<>();
        final Map<String, Set<AppDependency>> duplicateCatcher = new HashMap<>();
        final StringBuilder classPath = new StringBuilder();
        final Map<String, List<byte[]>> services = new HashMap<>();

        for (AppDependency appDep : appDeps) {
            if (appDep.getScope().equals(PROVIDED) && !augmentOutcome.isWhitelisted(appDep)) {
                continue;
            }
            final AppArtifact depArtifact = appDep.getArtifact();
            if (depArtifact.getArtifactId().equals("svm") && depArtifact.getGroupId().equals("com.oracle.substratevm")) {
                continue;
            }
            final Path resolvedDep = depResolver.resolve(depArtifact);
            if (uberJar) {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(resolvedDep)) {
                    for (final Path root : artifactFs.getRootDirectories()) {
                        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                                new SimpleFileVisitor<Path>() {
                                    @Override
                                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                            throws IOException {
                                        final String relativePath = root.relativize(dir).toString();
                                        if (!relativePath.isEmpty()) {
                                            addDir(runnerZipFs, dir, relativePath);
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                            throws IOException {
                                        final String relativePath = root.relativize(file).toString();
                                        if (relativePath.startsWith("META-INF/services/") && relativePath.length() > 18) {
                                            services.computeIfAbsent(relativePath, (u) -> new ArrayList<>()).add(read(file));
                                        } else if (!IGNORED_ENTRIES.contains(relativePath)) {
                                            duplicateCatcher.computeIfAbsent(relativePath, (a) -> new HashSet<>()).add(appDep);
                                            if (!seen.containsKey(relativePath)) {
                                                seen.put(relativePath, appDep.toString());
                                                Files.copy(file, runnerZipFs.getPath(relativePath),
                                                        StandardCopyOption.REPLACE_EXISTING);
                                            } else if (!relativePath.endsWith(".class")) {
                                                //for .class entries we warn as a group
                                                log.warn("Duplicate entry " + relativePath + " entry from " + appDep
                                                        + " will be ignored. Existing file was provided by "
                                                        + seen.get(relativePath));
                                            }
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }
                                });
                    }

                }
            } else {
                final String fileName = depArtifact.getGroupId() + "." + resolvedDep.getFileName();
                final Path targetPath = libDir.resolve(fileName);
                Files.copy(resolvedDep, targetPath, StandardCopyOption.REPLACE_EXISTING);
                classPath.append(" lib/" + fileName);
            }
        }
        Set<Set<AppDependency>> explained = new HashSet<>();
        for (Map.Entry<String, Set<AppDependency>> entry : duplicateCatcher.entrySet()) {
            if (entry.getValue().size() > 1) {
                if (explained.add(entry.getValue())) {
                    log.warn("Dependencies with duplicate files detected. The dependencies " + entry.getValue()
                            + " contain duplicate files, e.g. " + entry.getKey());
                }
            }
        }

        final Path wiringClassesDir = augmentOutcome.getWiringClassesDir();
        Files.walk(wiringClassesDir).forEach(new Consumer<Path>() {
            @Override
            public void accept(Path path) {
                try {
                    final String relativePath = wiringClassesDir.relativize(path).toString();
                    if (Files.isDirectory(path)) {
                        if (!seen.containsKey(relativePath + "/") && !relativePath.isEmpty()) {
                            seen.put(relativePath + "/", "Current Application");
                            addDir(runnerZipFs, path, relativePath);
                        }
                        return;
                    }
                    if (relativePath.startsWith("META-INF/services/") && relativePath.length() > 18) {
                        if (Files.size(path) > Integer.MAX_VALUE) {
                            throw new RuntimeException("Can't process class files larger than Integer.MAX_VALUE bytes");
                        }
                        services.computeIfAbsent(relativePath, (u) -> new ArrayList<>()).add(Files.readAllBytes(path));
                        return;
                    }
                    seen.put(relativePath, "Current Application");
                    Files.copy(path, runnerZipFs.getPath(relativePath), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath.toString());
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        try (OutputStream os = Files.newOutputStream(runnerZipFs.getPath("META-INF", "MANIFEST.MF"))) {
            manifest.write(os);
        }

        copyFiles(augmentOutcome.getAppClassesDir(), runnerZipFs);
        copyFiles(augmentOutcome.getTransformedClassesDir(), runnerZipFs);

        for (Map.Entry<String, List<byte[]>> entry : services.entrySet()) {
            try (OutputStream os = Files.newOutputStream(runnerZipFs.getPath(entry.getKey()))) {
                for (byte[] i : entry.getValue()) {
                    os.write(i);
                    os.write('\n');
                }
            }
        }
    }

    private void copyFiles(Path dir, FileSystem fs) throws IOException {
        Files.walk(dir).forEach(new Consumer<Path>() {
            @Override
            public void accept(Path path) {
                final String relativePath = dir.relativize(path).toString();
                if (relativePath.isEmpty()) {
                    return;
                }
                try {
                    if (Files.isDirectory(path)) {
                        addDir(fs, path, relativePath);
                    } else {
                        Files.copy(path, fs.getPath(relativePath), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void addDir(FileSystem fs, Path dir, final String relativePath)
            throws IOException, FileAlreadyExistsException {
        final Path targetDir = fs.getPath(relativePath);
        try {
            Files.copy(dir, targetDir);
        } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(targetDir)) {
                throw e;
            }
        }
    }

    private static byte[] read(Path p) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int r;
        try (InputStream in = Files.newInputStream(p)) {
            while ((r = in.read(buffer)) > 0) {
                out.write(buffer, 0, r);
            }
        }
        return out.toByteArray();
    }

    @Override
    public String getConfigPropertyName() {
        return "runner-jar";
    }

    @Override
    public PropertiesHandler<RunnerJarPhase> getPropertiesHandler() {
        return new MappedPropertiesHandler<RunnerJarPhase>() {
            @Override
            public RunnerJarPhase getTarget() {
                return RunnerJarPhase.this;
            }
        }
                .map("output", (RunnerJarPhase t, String value) -> t.setOutputDir(Paths.get(value)))
                .map("lib", (RunnerJarPhase t, String value) -> t.setLibDir(Paths.get(value)))
                .map("final-name", RunnerJarPhase::setFinalName)
                .map("main-class", RunnerJarPhase::setMainClass)
                .map("uber-jar", (RunnerJarPhase t, String value) -> t.setUberJar(Boolean.parseBoolean(value)));
    }
}
