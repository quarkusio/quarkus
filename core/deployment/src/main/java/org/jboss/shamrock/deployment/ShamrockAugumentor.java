package org.jboss.shamrock.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.builder.BuildChain;
import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildResult;
import org.jboss.builder.BuildStep;
import org.jboss.builder.item.BuildItem;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.ArchiveRootBuildItem;
import org.jboss.shamrock.deployment.builditem.ClassOutputBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.index.ApplicationArchiveLoader;

public class ShamrockAugumentor {

    private static final Logger log = Logger.getLogger(ShamrockAugumentor.class);

    private final List<Path> additionalApplicationArchives;
    private final ClassOutput output;
    private final ClassLoader classLoader;
    private final Path root;
    private final Set<Class<? extends BuildItem>> finalResults;

    ShamrockAugumentor(Builder builder) {
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        this.output = builder.output;
        this.classLoader = builder.classLoader;
        this.root = builder.root;
        this.finalResults = new HashSet<>(builder.finalResults);
    }

    public BuildResult run() throws Exception {
        long time = System.currentTimeMillis();
        log.info("Beginning shamrock augmentation");
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        //TODO: this needs to go away
        BuildConfig config = BuildConfig.readConfig(classLoader, root.toFile());

        Indexer indexer = new Indexer();
        Files.walkFileTree(root, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    try (InputStream stream = Files.newInputStream(file)) {
                        indexer.index(stream);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        Index appIndex = indexer.complete();
        List<ApplicationArchive> applicationArchives = ApplicationArchiveLoader.scanForOtherIndexes(classLoader, config, Collections.emptySet(), root, additionalApplicationArchives);


        BuildChainBuilder chainBuilder = BuildChain.builder()

                .loadProviders(Thread.currentThread().getContextClassLoader())
                .addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        //TODO: this should not be here
                        context.produce(new SubstrateResourceBuildItem("META-INF/microprofile-config.properties"));
                        context.produce(ShamrockConfig.INSTANCE);
                        context.produce(new ApplicationArchivesBuildItem(new ApplicationArchiveImpl(appIndex, root, null), applicationArchives));
                        context.produce(new ArchiveRootBuildItem(root));
                        context.produce(config);
                        context.produce(new ClassOutputBuildItem(output));
                    }
                })
                .produces(ShamrockConfig.class)
                .produces(ApplicationArchivesBuildItem.class)
                .produces(SubstrateResourceBuildItem.class)
                .produces(ArchiveRootBuildItem.class)
                .produces(BuildConfig.class)
                .produces(ClassOutputBuildItem.class)
                .build();
        for (Class<? extends BuildItem> i : finalResults) {
            chainBuilder.addFinal(i);
        }
        chainBuilder.addFinal(GeneratedClassBuildItem.class)
                .addFinal(GeneratedResourceBuildItem.class);

        BuildChain chain = chainBuilder
                .build();
        BuildResult buildResult = chain.createExecutionBuilder("main").execute();

        //TODO: this seems wrong
        for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
            output.writeClass(i.isApplicationClass(), i.getName(), i.getClassData());
        }
        for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
            output.writeResource(i.getName(), i.getClassData());
        }
        log.info("Shamrock augmentation completed in " + (System.currentTimeMillis() - time) + "ms");
        return buildResult;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        List<Path> additionalApplicationArchives = new ArrayList<>();
        ClassOutput output;
        ClassLoader classLoader;
        Path root;
        Set<Class<? extends BuildItem>> finalResults = new HashSet<>();


        public List<Path> getAdditionalApplicationArchives() {
            return additionalApplicationArchives;
        }

        public Builder addAdditionalApplicationArchive(Path archive) {
            this.additionalApplicationArchives.add(archive);
            return this;
        }

        public ClassOutput getOutput() {
            return output;
        }

        public Builder setOutput(ClassOutput output) {
            this.output = output;
            return this;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Path getRoot() {
            return root;
        }

        public <T extends BuildItem> Builder addFinal(Class<T> clazz) {
            finalResults.add(clazz);
            return this;
        }

        public Builder setRoot(Path root) {
            this.root = root;
            return this;
        }

        public ShamrockAugumentor build() {
            return new ShamrockAugumentor(this);
        }
    }
}
