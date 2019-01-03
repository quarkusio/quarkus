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

package org.jboss.shamrock.creator.phase.augment;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.microprofile.config.Config;
import org.jboss.builder.BuildResult;
import org.jboss.logging.Logger;
import org.jboss.shamrock.creator.AppArtifact;
import org.jboss.shamrock.creator.AppArtifactResolver;
import org.jboss.shamrock.creator.AppCreationContext;
import org.jboss.shamrock.creator.AppCreationPhase;
import org.jboss.shamrock.creator.AppCreatorException;
import org.jboss.shamrock.creator.AppDependency;
import org.jboss.shamrock.creator.phase.runnerjar.RunnerJarOutcome;
import org.jboss.shamrock.creator.util.IoUtils;
import org.jboss.shamrock.creator.util.ZipUtils;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.deployment.ShamrockAugmentor;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.MainClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateOutputBuildItem;
import org.jboss.shamrock.deployment.index.ResolvedArtifact;
import org.jboss.shamrock.dev.CopyUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * This phase at the moment actually combines augmentation and runnable JAR building.
 *
 * @author Alexey Loubyansky
 */
public class AugmentPhase implements AppCreationPhase, AugmentOutcome, RunnerJarOutcome {

    private static final String DEFAULT_MAIN_CLASS = "org.jboss.shamrock.runner.GeneratedMain";
    private static final String DEPENDENCIES_RUNTIME = "dependencies.runtime";
    private static final String PROVIDED = "provided";

    private static final Logger log = Logger.getLogger(AugmentPhase.class);

    private Path outputDir;
    private Path appClassesDir;
    private Path wiringClassesDir;
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
     * @param outputDir  output directory for this phase
     * @return  this phase instance
     */
    public AugmentPhase setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /**
     * Directory containing application classes. If none is set by the user,
     * the creation process has to be initiated with an application JAR which
     * will be unpacked into classes directory in the creator's work directory.
     *
     * @param appClassesDir  directory for application classes
     * @return  this phase instance
     */
    public AugmentPhase setAppClassesDir(Path appClassesDir) {
        this.appClassesDir = appClassesDir;
        return this;
    }

    /**
     * The directory for generated classes. If none is set by the user,
     * wiring-classes directory will be created in the work directory of the creator.
     *
     * @param wiringClassesDir  directory for generated classes
     * @return  this phase instance
     */
    public AugmentPhase setWiringClassesDir(Path wiringClassesDir) {
        this.wiringClassesDir = wiringClassesDir;
        return this;
    }

    /**
     * Directory for application dependencies. If none set by the user
     * lib directory will be created in the output directory of the phase.
     *
     * @param libDir  directory for project dependencies
     * @return  this phase instance
     */
    public AugmentPhase setLibDir(Path libDir) {
        this.libDir = libDir;
        return this;
    }

    /**
     * Name for the runnable JAR. If none is provided by the user
     * the name will derived from the user application JAR filename.
     *
     * @param finalName  runnable JAR name
     * @return  this phase instance
     */
    public AugmentPhase setFinalName(String finalName) {
        this.finalName = finalName;
        return this;
    }

    /**
     * Main class name fir the runnable JAR. If none is set by the user
     * org.jboss.shamrock.runner.GeneratedMain will be use by default.
     *
     * @param mainClass  main class name for the runnable JAR
     * @return
     */
    public AugmentPhase setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Whether to build an uber JAR. The default is false.
     *
     * @param uberJar  whether to build an uber JAR
     * @return  this phase instance
     */
    public AugmentPhase setUberJar(boolean uberJar) {
        this.uberJar = uberJar;
        return this;
    }

    @Override
    public Path getAppClassesDir() {
        return appClassesDir;
    }

    @Override
    public Path getWiringClassesDir() {
        return wiringClassesDir;
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
    public void process(AppCreationContext ctx) throws AppCreatorException {
        outputDir = outputDir == null ? ctx.getWorkPath() : IoUtils.mkdirs(outputDir);

        if (appClassesDir == null) {
            appClassesDir = ctx.createWorkDir("classes");
            final Path appJar = ctx.getArtifactResolver().resolve(ctx.getAppArtifact());
            try {
                ZipUtils.unzip(appJar, appClassesDir);
            } catch (IOException e) {
                throw new AppCreatorException("Failed to unzip " + appJar, e);
            }
            final Path metaInf = appClassesDir.resolve("META-INF");
            IoUtils.recursiveDelete(metaInf.resolve("maven"));
            IoUtils.recursiveDelete(metaInf.resolve("INDEX.LIST"));
            IoUtils.recursiveDelete(metaInf.resolve("MANIFEST.MF"));
        }

        wiringClassesDir = IoUtils.mkdirs(wiringClassesDir == null ? ctx.getWorkPath("wiring-classes") : wiringClassesDir);

        libDir = IoUtils.mkdirs(libDir == null ? outputDir.resolve("lib") : libDir);

        if (finalName == null) {
            final String name = ctx.getArtifactResolver().resolve(ctx.getAppArtifact()).getFileName().toString();
            int i = name.lastIndexOf('.');
            if (i > 0) {
                finalName = name.substring(0, i);
            }
        }

        doProcess(ctx);

        ctx.pushOutcome(AugmentOutcome.class, this);
        ctx.pushOutcome(RunnerJarOutcome.class, this);
    }

    private void doProcess(AppCreationContext ctx) throws AppCreatorException {
        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        Path config = appClassesDir.resolve("META-INF").resolve("microprofile-config.properties");
        if(Files.exists(config)) {
            try {
                Config built = SmallRyeConfigProviderResolver.instance().getBuilder()
                        .addDefaultSources()
                        .addDiscoveredConverters()
                        .addDiscoveredSources()
                        .withSources(new PropertiesConfigSource(config.toUri().toURL())).build();
                SmallRyeConfigProviderResolver.instance().registerConfig(built, Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final AppArtifactResolver depResolver = ctx.getArtifactResolver();
        final List<AppDependency> appDeps = depResolver.collectDependencies(ctx.getAppArtifact());

        try {
            StringBuilder classPath = new StringBuilder();
            List<String> problems = new ArrayList<>();
            Set<String> whitelist = new HashSet<>();
            for (AppDependency appDep : appDeps) {
                final AppArtifact depCoords = appDep.getArtifact();
                if (!"jar".equals(depCoords.getType())) {
                    continue;
                }
                try (ZipFile zip = openZipFile(depResolver.resolve(depCoords))) {
                    if (!appDep.getScope().equals(PROVIDED) && zip.getEntry("META-INF/services/org.jboss.shamrock.deployment.ShamrockSetup") != null) {
                        problems.add("Artifact " + appDep + " is a deployment artifact, however it does not have scope required. This will result in unnecessary jars being included in the final image");
                    }
                    ZipEntry deps = zip.getEntry(DEPENDENCIES_RUNTIME);
                    if (deps != null) {
                        whitelist.add(getDependencyConflictId(appDep.getArtifact()));
                        try (InputStream in = zip.getInputStream(deps)) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.trim().split(":");
                                if (parts.length < 5) {
                                    continue;
                                }
                                StringBuilder sb = new StringBuilder();
                                //the last two bits are version and scope
                                //which we don't want
                                for (int i = 0; i < parts.length - 2; ++i) {
                                    if (i > 0) {
                                        sb.append(':');
                                    }
                                    sb.append(parts[i]);
                                }
                                whitelist.add(sb.toString());
                            }
                        }
                    }

                }
            }
            if (!problems.isEmpty()) {
                //TODO: add a config option to just log an error instead
                throw new AppCreatorException(problems.toString());
            }
            Set<String> seen = new HashSet<>();
            runnerJar = outputDir.resolve(finalName + "-runner.jar");
            log.info("Building jar: " + runnerJar);
            try (ZipOutputStream runner = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(runnerJar)))) {
                Map<String, List<byte[]>> services = new HashMap<>();

                for (AppDependency appDep : appDeps) {
                    final AppArtifact depCoords = appDep.getArtifact();
                    if (appDep.getScope().equals(PROVIDED) && !whitelist.contains(getDependencyConflictId(depCoords))) {
                        continue;
                    }
                    if (depCoords.getArtifactId().equals("svm") && depCoords.getGroupId().equals("com.oracle.substratevm")) {
                        continue;
                    }
                    final File artifactFile = depResolver.resolve(depCoords).toFile();
                    if (uberJar) {
                        try (ZipInputStream in = new ZipInputStream(new FileInputStream(artifactFile))) {
                            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                                if (e.getName().startsWith("META-INF/services/") && e.getName().length() > 18) {
                                    services.computeIfAbsent(e.getName(), (u) -> new ArrayList<>()).add(read(in));
                                    continue;
                                } else if (e.getName().equals("META-INF/MANIFEST.MF")) {
                                    continue;
                                }
                                if (!seen.add(e.getName())) {
                                    if (!e.getName().endsWith("/")) {
                                        log.warn("Duplicate entry " + e.getName() + " entry from " + appDep + " will be ignored");
                                    }
                                    continue;
                                }
                                runner.putNextEntry(new ZipEntry(e.getName()));
                                doCopy(runner, in);
                            }
                        }
                    } else {
                        final String fileName = depCoords.getGroupId() + "." + artifactFile.getName();
                        final Path targetPath = libDir.resolve(fileName);

                        Files.copy(artifactFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                        classPath.append(" lib/" + fileName);
                    }
                }

                List<URL> classPathUrls = new ArrayList<>();
                for (AppDependency appDep : appDeps) {
                    final AppArtifact depCoords = appDep.getArtifact();
                    final Path p = depResolver.resolve(depCoords);
                    classPathUrls.add(p.toUri().toURL());
                }

                //we need to make sure all the deployment artifacts are on the class path
                //to do this we need to create a new class loader to actually use for the runner
                List<URL> cpCopy = new ArrayList<>();

                cpCopy.add(appClassesDir.toUri().toURL());
                cpCopy.addAll(classPathUrls);

                URLClassLoader runnerClassLoader = new URLClassLoader(cpCopy.toArray(new URL[0]), getClass().getClassLoader());
                final Path wiringClassesDirectory = wiringClassesDir;
                ClassOutput classOutput = new ClassOutput() {
                    @Override
                    public void writeClass(boolean applicationClass, String className, byte[] data) throws IOException {
                        String location = className.replace('.', '/');
                        final Path p = wiringClassesDirectory.resolve(location + ".class");
                        Files.createDirectories(p.getParent());
                        try (OutputStream out = Files.newOutputStream(p)) {
                            out.write(data);
                        }
                    }

                    @Override
                    public void writeResource(String name, byte[] data) throws IOException {
                        final Path p = wiringClassesDirectory.resolve(name);
                        Files.createDirectories(p.getParent());
                        try (OutputStream out = Files.newOutputStream(p)) {
                            out.write(data);
                        }
                    }
                };


                ClassLoader old = Thread.currentThread().getContextClassLoader();
                BuildResult result;
                try {
                    Thread.currentThread().setContextClassLoader(runnerClassLoader);

                    ShamrockAugmentor.Builder builder = ShamrockAugmentor.builder();
                    builder.setRoot(appClassesDir);
                    builder.setClassLoader(runnerClassLoader);
                    builder.setOutput(classOutput);
                    builder.addFinal(BytecodeTransformerBuildItem.class)
                            .addFinal(MainClassBuildItem.class)
                            .addFinal(SubstrateOutputBuildItem.class);
                    result = builder.build().run();
                } finally {
                    Thread.currentThread().setContextClassLoader(old);
                }

                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = new HashMap<>();
                List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result.consumeMulti(BytecodeTransformerBuildItem.class);
                if (!bytecodeTransformerBuildItems.isEmpty()) {
                    for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                        bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>()).add(i.getVisitorFunction());
                    }
                }

                Files.walk(wiringClassesDirectory).forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path path) {
                        try {
                            String pathName = wiringClassesDirectory.relativize(path).toString();
                            if (Files.isDirectory(path)) {
                                String p = pathName + "/";
                                if (seen.contains(p)) {
                                    return;
                                }
                                seen.add(p);
                                if (!pathName.isEmpty()) {
                                    runner.putNextEntry(new ZipEntry(p));
                                }
                            } else if (pathName.startsWith("META-INF/services/") && pathName.length() > 18) {
                                services.computeIfAbsent(pathName, (u) -> new ArrayList<>()).add(CopyUtils.readFileContent(path));
                            } else {
                                seen.add(pathName);
                                runner.putNextEntry(new ZipEntry(pathName));
                                try (FileInputStream in = new FileInputStream(path.toFile())) {
                                    doCopy(runner, in);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath.toString());
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
                runner.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                manifest.write(runner);
                //now copy all the contents to the runner jar
                //I am not 100% sure about this idea, but if we are going to support bytecode transforms it seems
                //like the cleanest way to do it
                //at the end of the PoC phase all this needs review
                Path appJar = appClassesDir;
                ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                ConcurrentLinkedDeque<Future<FutureEntry>> transformed = new ConcurrentLinkedDeque<>();
                try {
                    Files.walk(appJar).forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            try {
                                final String pathName = appJar.relativize(path).toString();
                                if (Files.isDirectory(path)) {
//                                if (!pathName.isEmpty()) {
//                                    out.putNextEntry(new ZipEntry(pathName + "/"));
//                                }
                                } else if (pathName.endsWith(".class") && !bytecodeTransformers.isEmpty()) {
                                    String className = pathName.substring(0, pathName.length() - 6).replace('/', '.');
                                    List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = bytecodeTransformers.get(className);

                                    if (visitors == null || visitors.isEmpty()) {
                                        runner.putNextEntry(new ZipEntry(pathName));
                                        try (FileInputStream in = new FileInputStream(path.toFile())) {
                                            doCopy(runner, in);
                                        }
                                    } else {
                                        transformed.add(executorPool.submit(new Callable<FutureEntry>() {
                                            @Override
                                            public FutureEntry call() throws Exception {
                                                final byte[] fileContent = CopyUtils.readFileContent(path);
                                                ClassReader cr = new ClassReader(fileContent);
                                                ClassWriter writer = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                                ClassVisitor visitor = writer;
                                                for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                                    visitor = i.apply(className, visitor);
                                                }
                                                cr.accept(visitor, 0);
                                                return new FutureEntry(writer.toByteArray(), pathName);
                                            }
                                        }));
                                    }
                                } else {
                                    runner.putNextEntry(new ZipEntry(pathName));
                                    try (FileInputStream in = new FileInputStream(path.toFile())) {
                                        doCopy(runner, in);
                                    }
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    for (Future<FutureEntry> i : transformed) {

                        FutureEntry res = i.get();
                        runner.putNextEntry(new ZipEntry(res.location));
                        runner.write(res.data);
                    }
                } finally {
                    executorPool.shutdown();
                }
                for (Map.Entry<String, List<byte[]>> entry : services.entrySet()) {
                    runner.putNextEntry(new ZipEntry(entry.getKey()));
                    for (byte[] i : entry.getValue()) {
                        runner.write(i);
                        runner.write('\n');
                    }
                }
            }
        } catch (Exception e) {
            throw new AppCreatorException("Failed to run", e);
        }
    }

    private static String getDependencyConflictId(AppArtifact coords) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(coords.getGroupId());
        sb.append(':');
        sb.append(coords.getArtifactId());
        sb.append(':');
        sb.append(coords.getType());
        if (!coords.getClassifier().isEmpty()) {
            sb.append(':');
            sb.append(coords.getClassifier());
        }
        return sb.toString();
    }

    private ZipFile openZipFile(Path p) {
        if (!Files.isReadable(p)) {
            throw new RuntimeException("File not existing or not allowed for reading: " + p);
        }
        try {
            return new ZipFile(p.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Error opening zip stream from artifact: " + p);
        }
    }

    private static void doCopy(OutputStream out, InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        int r;
        while ((r = in.read(buffer)) > 0) {
            out.write(buffer, 0, r);
        }
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int r;
        while ((r = in.read(buffer)) > 0) {
            out.write(buffer, 0, r);
        }
        return out.toByteArray();
    }

    private static final class FutureEntry {
        final byte[] data;
        final String location;

        private FutureEntry(byte[] data, String location) {
            this.data = data;
            this.location = location;
        }
    }
}
