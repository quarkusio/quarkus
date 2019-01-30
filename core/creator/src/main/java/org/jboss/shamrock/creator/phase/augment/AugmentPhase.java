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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.microprofile.config.Config;
import org.jboss.builder.BuildResult;
import org.jboss.logging.Logger;
import org.jboss.shamrock.creator.AppArtifact;
import org.jboss.shamrock.creator.AppArtifactResolver;
import org.jboss.shamrock.creator.AppCreationPhase;
import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.AppCreatorException;
import org.jboss.shamrock.creator.AppDependency;
import org.jboss.shamrock.creator.config.reader.MappedPropertiesHandler;
import org.jboss.shamrock.creator.config.reader.PropertiesHandler;
import org.jboss.shamrock.creator.outcome.OutcomeProviderRegistration;
import org.jboss.shamrock.creator.phase.curate.CurateOutcome;
import org.jboss.shamrock.creator.util.IoUtils;
import org.jboss.shamrock.creator.util.ZipUtils;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.deployment.ShamrockAugmentor;
import org.jboss.shamrock.deployment.ShamrockClassWriter;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.MainClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateOutputBuildItem;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * This phase consumes {@link org.jboss.shamrock.creator.phase.curate.CurateOutcome} and processes
 * user application and and its dependency classes for phases that generate a runnable application.
 *
 * @author Alexey Loubyansky
 */
public class AugmentPhase implements AppCreationPhase<AugmentPhase>, AugmentOutcome {

    private static final String DEPENDENCIES_RUNTIME = "dependencies.runtime";
    private static final String PROVIDED = "provided";

    private static final Logger log = Logger.getLogger(AugmentPhase.class);

    private Path outputDir;
    private Path appClassesDir;
    private Path transformedClassesDir;
    private Path wiringClassesDir;
    private Set<String> whitelist = new HashSet<>();

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
     * Directory containing transformed application classes. If none is set by
     * the user, transformed-classes directory will be created in the work
     * directory of the creator.
     *
     * @param transformedClassesDir  directory for transformed application classes
     * @return  this phase instance
     */
    public AugmentPhase setTransformedClassesDir(Path transformedClassesDir) {
        this.transformedClassesDir = transformedClassesDir;
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

    @Override
    public Path getAppClassesDir() {
        return appClassesDir;
    }

    @Override
    public Path getTransformedClassesDir() {
        return transformedClassesDir;
    }

    @Override
    public Path getWiringClassesDir() {
        return wiringClassesDir;
    }

    @Override
    public boolean isWhitelisted(AppDependency dep) {
        return whitelist.contains(getDependencyConflictId(dep.getArtifact()));
    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(AugmentOutcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {
        final CurateOutcome appState = ctx.resolveOutcome(CurateOutcome.class);

        outputDir = outputDir == null ? ctx.getWorkPath() : IoUtils.mkdirs(outputDir);

        if (appClassesDir == null) {
            appClassesDir = outputDir.resolve("classes");
            final Path appJar = appState.getArtifactResolver().resolve(appState.getAppArtifact());
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

        transformedClassesDir = IoUtils.mkdirs(transformedClassesDir == null ? outputDir.resolve("transformed-classes") : transformedClassesDir);
        wiringClassesDir = IoUtils.mkdirs(wiringClassesDir == null ? outputDir.resolve("wiring-classes") : wiringClassesDir);

        doProcess(appState);

        ctx.pushOutcome(AugmentOutcome.class, this);
    }

    private void doProcess(CurateOutcome appState) throws AppCreatorException {
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

        final AppArtifactResolver depResolver = appState.getArtifactResolver();
        final List<AppDependency> appDeps = appState.getEffectiveDeps();

        try {
            // we need to make sure all the deployment artifacts are on the class path
            final List<URL> cpUrls = new ArrayList<>();
            cpUrls.add(appClassesDir.toUri().toURL());

            List<String> problems = null;
            for (AppDependency appDep : appDeps) {
                final AppArtifact depArtifact = appDep.getArtifact();
                final Path resolvedDep = depResolver.resolve(depArtifact);
                cpUrls.add(resolvedDep.toUri().toURL());

                if (!"jar".equals(depArtifact.getType())) {
                    continue;
                }
                try (ZipFile zip = openZipFile(resolvedDep)) {
                    if (!appDep.getScope().equals(PROVIDED) && zip.getEntry("META-INF/services/org.jboss.shamrock.deployment.ShamrockSetup") != null) {
                        if(problems == null) {
                            problems = new ArrayList<>();
                        }
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
            if (problems != null) {
                //TODO: add a config option to just log an error instead
                throw new AppCreatorException(problems.toString());
            }

            final URLClassLoader runnerClassLoader = new URLClassLoader(cpUrls.toArray(new URL[cpUrls.size()]), getClass().getClassLoader());
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
                builder.addFinal(BytecodeTransformerBuildItem.class).addFinal(MainClassBuildItem.class)
                        .addFinal(SubstrateOutputBuildItem.class);
                result = builder.build().run();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

            final List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result.consumeMulti(BytecodeTransformerBuildItem.class);
            if (!bytecodeTransformerBuildItems.isEmpty()) {
                final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = new HashMap<>(bytecodeTransformerBuildItems.size());
                if (!bytecodeTransformerBuildItems.isEmpty()) {
                    for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                        bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>()).add(i.getVisitorFunction());
                    }
                }

                // now copy all the contents to the runner jar
                // I am not 100% sure about this idea, but if we are going to support bytecode transforms it seems
                // like the cleanest way to do it
                // at the end of the PoC phase all this needs review
                final ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                final ConcurrentLinkedDeque<Future<FutureEntry>> transformed = new ConcurrentLinkedDeque<>();
                try {
                    Files.walk(appClassesDir).forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            if (Files.isDirectory(path)) {
                                return;
                            }
                            final String pathName = appClassesDir.relativize(path).toString();
                            if (!pathName.endsWith(".class") || bytecodeTransformers.isEmpty()) {
                                return;
                            }
                            final String className = pathName.substring(0, pathName.length() - 6).replace('/', '.');
                            final List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = bytecodeTransformers.get(className);
                            if (visitors == null || visitors.isEmpty()) {
                                return;
                            }
                            transformed.add(executorPool.submit(new Callable<FutureEntry>() {
                                @Override
                                public FutureEntry call() throws Exception {
                                    if (Files.size(path) > Integer.MAX_VALUE) {
                                        throw new RuntimeException("Can't process class files larger than Integer.MAX_VALUE bytes");
                                    }
                                    ClassReader cr = new ClassReader(Files.readAllBytes(path));
                                    ClassWriter writer = new ShamrockClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                    ClassVisitor visitor = writer;
                                    for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                        visitor = i.apply(className, visitor);
                                    }
                                    cr.accept(visitor, 0);
                                    return new FutureEntry(writer.toByteArray(), pathName);
                                }
                            }));
                        }
                    });
                } finally {
                    executorPool.shutdown();
                }
                if (!transformed.isEmpty()) {
                    for (Future<FutureEntry> i : transformed) {
                        final FutureEntry res = i.get();
                        final Path classFile = transformedClassesDir.resolve(res.location);
                        Files.createDirectories(classFile.getParent());
                        try(OutputStream out = Files.newOutputStream(classFile)) {
                            IoUtils.copy(out, new ByteArrayInputStream(res.data));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new AppCreatorException("Failed to augment application classes", e);
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

    private static final class FutureEntry {
        final byte[] data;
        final String location;

        private FutureEntry(byte[] data, String location) {
            this.data = data;
            this.location = location;
        }
    }

    @Override
    public String getConfigPropertyName() {
        return "augment";
    }

    @Override
    public PropertiesHandler<AugmentPhase> getPropertiesHandler() {
        return new MappedPropertiesHandler<AugmentPhase>() {
            @Override
            public AugmentPhase getTarget() {
                return AugmentPhase.this;
            }
        }
        .map("output", (AugmentPhase t, String value) -> t.setOutputDir(Paths.get(value)))
        .map("classes", (AugmentPhase t, String value) -> t.setAppClassesDir(Paths.get(value)))
        .map("transformed-classes", (AugmentPhase t, String value) -> t.setTransformedClassesDir(Paths.get(value)))
        .map("wiring-classes", (AugmentPhase t, String value) -> t.setWiringClassesDir(Paths.get(value)));
    }
}
