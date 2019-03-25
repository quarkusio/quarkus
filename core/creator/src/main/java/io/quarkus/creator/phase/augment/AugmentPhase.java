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

package io.quarkus.creator.phase.augment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;
import org.jboss.builder.BuildResult;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.creator.AppCreationPhase;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.phase.curate.CurateOutcome;
import io.quarkus.deployment.ApplicationInfoUtil;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateOutputBuildItem;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * This phase consumes {@link io.quarkus.creator.phase.curate.CurateOutcome} and processes
 * user application and and its dependency classes for phases that generate a runnable application.
 *
 * @author Alexey Loubyansky
 */
public class AugmentPhase implements AppCreationPhase<AugmentPhase>, AugmentOutcome {

    private static final Logger log = Logger.getLogger(AugmentPhase.class);
    private static final String APPLICATION_INFO_PROPERTIES = "application-info.properties";
    private static final String META_INF = "META-INF";

    private Path outputDir;
    private Path appClassesDir;
    private Path transformedClassesDir;
    private Path wiringClassesDir;
    private Path configDir;

    /**
     * Output directory for the outcome of this phase.
     * If not set by the user the work directory of the creator
     * will be used instead.
     *
     * @param outputDir output directory for this phase
     * @return this phase instance
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
     * @param appClassesDir directory for application classes
     * @return this phase instance
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
     * @param transformedClassesDir directory for transformed application classes
     * @return this phase instance
     */
    public AugmentPhase setTransformedClassesDir(Path transformedClassesDir) {
        this.transformedClassesDir = transformedClassesDir;
        return this;
    }

    /**
     * The directory for generated classes. If none is set by the user,
     * wiring-classes directory will be created in the work directory of the creator.
     *
     * @param wiringClassesDir directory for generated classes
     * @return this phase instance
     */
    public AugmentPhase setWiringClassesDir(Path wiringClassesDir) {
        this.wiringClassesDir = wiringClassesDir;
        return this;
    }

    /**
     * Directory containing the configuration files.
     *
     * @param configDir directory the configuration files (application.properties)
     * @return this phase instance
     */
    public AugmentPhase setConfigDir(Path configDir) {
        this.configDir = configDir;
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
    public Path getConfigDir() {
        return configDir;
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
            Path appJar = appState.getAppArtifact().getPath();
            try {
                ZipUtils.unzip(appJar, appClassesDir);
            } catch (IOException e) {
                throw new AppCreatorException("Failed to unzip " + appJar, e);
            }
            final Path metaInf = appClassesDir.resolve(META_INF);
            IoUtils.recursiveDelete(metaInf.resolve("maven"));
            IoUtils.recursiveDelete(metaInf.resolve("INDEX.LIST"));
            IoUtils.recursiveDelete(metaInf.resolve("MANIFEST.MF"));
            IoUtils.recursiveDelete(metaInf.resolve(APPLICATION_INFO_PROPERTIES));
        }

        ApplicationInfoUtil.writeApplicationInfoProperties(appState.getAppArtifact(), appClassesDir);

        //lets default to appClassesDir for now
        if (configDir == null)
            configDir = appClassesDir;

        transformedClassesDir = IoUtils
                .mkdirs(transformedClassesDir == null ? outputDir.resolve("transformed-classes") : transformedClassesDir);
        wiringClassesDir = IoUtils.mkdirs(wiringClassesDir == null ? outputDir.resolve("wiring-classes") : wiringClassesDir);

        doProcess(appState);

        ctx.pushOutcome(AugmentOutcome.class, this);
    }

    private void doProcess(CurateOutcome appState) throws AppCreatorException {
        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        Path config = configDir.resolve("application.properties");
        if (Files.exists(config)) {
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

        final AppModelResolver depResolver = appState.getArtifactResolver();
        List<AppDependency> appDeps;
        try {
            appDeps = appState.getEffectiveModel().getAllDependencies();
        } catch (BootstrapDependencyProcessingException e) {
            throw new AppCreatorException("Failed to resolve application build classpath", e);
        }
        URLClassLoader runnerClassLoader = null;
        try {
            // we need to make sure all the deployment artifacts are on the class path
            final List<URL> cpUrls = new ArrayList<>(appDeps.size() + 1);
            cpUrls.add(appClassesDir.toUri().toURL());

            for (AppDependency appDep : appDeps) {
                final Path resolvedDep = depResolver.resolve(appDep.getArtifact());
                cpUrls.add(resolvedDep.toUri().toURL());
            }

            runnerClassLoader = new URLClassLoader(cpUrls.toArray(new URL[cpUrls.size()]), getClass().getClassLoader());

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

                QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder();
                builder.setRoot(appClassesDir);
                builder.setClassLoader(runnerClassLoader);
                builder.setOutput(classOutput);
                builder.addFinal(BytecodeTransformerBuildItem.class).addFinal(MainClassBuildItem.class)
                        .addFinal(SubstrateOutputBuildItem.class);
                result = builder.build().run();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

            final List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result
                    .consumeMulti(BytecodeTransformerBuildItem.class);
            if (!bytecodeTransformerBuildItems.isEmpty()) {
                final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = new HashMap<>(
                        bytecodeTransformerBuildItems.size());
                if (!bytecodeTransformerBuildItems.isEmpty()) {
                    for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                        bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>())
                                .add(i.getVisitorFunction());
                    }
                }

                // now copy all the contents to the runner jar
                // I am not 100% sure about this idea, but if we are going to support bytecode transforms it seems
                // like the cleanest way to do it
                // at the end of the PoC phase all this needs review
                final ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                final ConcurrentLinkedDeque<Future<FutureEntry>> transformed = new ConcurrentLinkedDeque<>();
                try {
                    ClassLoader transformCl = runnerClassLoader;
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
                            final List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = bytecodeTransformers
                                    .get(className);
                            if (visitors == null || visitors.isEmpty()) {
                                return;
                            }
                            transformed.add(executorPool.submit(new Callable<FutureEntry>() {
                                @Override
                                public FutureEntry call() throws Exception {
                                    ClassLoader old = Thread.currentThread().getContextClassLoader();
                                    try {
                                        Thread.currentThread().setContextClassLoader(transformCl);
                                        if (Files.size(path) > Integer.MAX_VALUE) {
                                            throw new RuntimeException(
                                                    "Can't process class files larger than Integer.MAX_VALUE bytes");
                                        }
                                        ClassReader cr = new ClassReader(Files.readAllBytes(path));
                                        ClassWriter writer = new QuarkusClassWriter(cr,
                                                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                        ClassVisitor visitor = writer;
                                        for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                            visitor = i.apply(className, visitor);
                                        }
                                        cr.accept(visitor, 0);
                                        return new FutureEntry(writer.toByteArray(), pathName);
                                    } finally {
                                        Thread.currentThread().setContextClassLoader(old);
                                    }
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
                        try (OutputStream out = Files.newOutputStream(classFile)) {
                            IoUtils.copy(out, new ByteArrayInputStream(res.data));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new AppCreatorException("Failed to augment application classes", e);
        } finally {
            if (runnerClassLoader != null) {
                try {
                    runnerClassLoader.close();
                } catch (IOException e) {
                    log.warn("Failed to close runner classloader", e);
                }
            }
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
                .map("wiring-classes", (AugmentPhase t, String value) -> t.setWiringClassesDir(Paths.get(value)))
                .map("config", (AugmentPhase t, String value) -> t.setConfigDir(Paths.get(value)));
    }
}
