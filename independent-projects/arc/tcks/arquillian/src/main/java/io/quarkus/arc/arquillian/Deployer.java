package io.quarkus.arc.arquillian;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import io.quarkus.arc.arquillian.utils.Archives;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.bcextensions.ExtensionsEntryPoint;

final class Deployer {
    private final Archive<?> deploymentArchive;
    private final DeploymentDir deploymentDir;
    private final String testClass;

    private final List<BeanArchive> beanArchives = new ArrayList<>();

    Deployer(Archive<?> deploymentArchive, DeploymentDir deploymentDir, String testClass) {
        this.deploymentArchive = deploymentArchive;
        this.deploymentDir = deploymentDir;
        this.testClass = testClass;
    }

    DeploymentClassLoader deploy() throws DeploymentException {
        try {
            if (deploymentArchive instanceof JavaArchive) {
                explodeJar();
            } else if (deploymentArchive instanceof WebArchive) {
                explodeWar();
            } else {
                throw new DeploymentException("Unknown archive type: " + deploymentArchive);
            }

            generate();

            return new DeploymentClassLoader(deploymentDir);
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new DeploymentException("Deployment failed", e);
        }
    }

    private void explodeJar() throws IOException {
        Archives.explode(deploymentArchive, "/", deploymentDir.appClasses);

        BeanArchive beanArchive = BeanArchive.detect(deploymentArchive);
        if (beanArchive != null) {
            beanArchives.add(beanArchive);
        }
    }

    private void explodeWar() throws IOException {
        Archives.explode(deploymentArchive, "/WEB-INF/classes/", deploymentDir.appClasses);

        BeanArchive beanArchive = BeanArchive.detect(deploymentArchive);
        if (beanArchive != null) {
            beanArchives.add(beanArchive);
        }

        Map<ArchivePath, Node> libs = deploymentArchive.getContent(Filters.include("^/WEB-INF/lib/.*\\.jar$"));
        for (Map.Entry<ArchivePath, Node> entry : libs.entrySet()) {
            String path = entry.getKey().get();
            Asset asset = entry.getValue().getAsset();

            String jarFile = path.replace("/WEB-INF/lib/", "");
            Path jarFilePath = deploymentDir.appLibraries.resolve(jarFile);
            Archives.copy(asset, jarFilePath);

            if (asset instanceof ArchiveAsset) {
                BeanArchive nestedBeanArchive = BeanArchive.detect(((ArchiveAsset) asset).getArchive());
                if (nestedBeanArchive != null) {
                    beanArchives.add(nestedBeanArchive);
                }
            }
        }
    }

    private void generate() throws IOException, ExecutionException, InterruptedException {
        Index applicationIndex = buildApplicationIndex();

        try (Closeable ignored = withDeploymentClassLoader()) {
            ExtensionsEntryPoint buildCompatibleExtensions = new ExtensionsEntryPoint();
            Set<String> additionalClasses = new HashSet<>();
            buildCompatibleExtensions.runDiscovery(applicationIndex, additionalClasses);

            IndexView beanArchiveIndex = buildImmutableBeanArchiveIndex(applicationIndex, additionalClasses);

            BeanProcessor beanProcessor = BeanProcessor.builder()
                    .setName(deploymentDir.root.getFileName().toString())
                    .setImmutableBeanArchiveIndex(beanArchiveIndex)
                    .setComputingBeanArchiveIndex(BeanArchives.buildComputingBeanArchiveIndex(
                            Thread.currentThread().getContextClassLoader(), new ConcurrentHashMap<>(),
                            beanArchiveIndex))
                    .setApplicationIndex(applicationIndex)
                    .setStrictCompatibility(true)
                    .setTransformUnproxyableClasses(false)
                    .setRemoveUnusedBeans(false)
                    .setBuildCompatibleExtensions(buildCompatibleExtensions)
                    .setAdditionalBeanDefiningAnnotations(Set.of(
                            new BeanDefiningAnnotation(DotName.createSimple(ExtraBean.class))))
                    .addAnnotationTransformer(new AnnotationsTransformer() {
                        @Override
                        public boolean appliesTo(AnnotationTarget.Kind kind) {
                            return kind == AnnotationTarget.Kind.CLASS;
                        }

                        @Override
                        public void transform(TransformationContext ctx) {
                            if (testClass.equals(ctx.getTarget().asClass().name().toString())) {
                                // make the test class a bean
                                ctx.transform().add(ExtraBean.class).done();
                            }
                            if (additionalClasses.contains(ctx.getTarget().asClass().name().toString())) {
                                // make all the `@Discovery`-registered classes beans
                                ctx.transform().add(ExtraBean.class).done();
                            }
                        }
                    })
                    .setOutput(resource -> {
                        switch (resource.getType()) {
                            case JAVA_CLASS:
                                resource.writeTo(deploymentDir.generatedClasses.toFile());
                                break;
                            case SERVICE_PROVIDER:
                                resource.writeTo(deploymentDir.generatedServices.toFile());
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown resource type " + resource.getType());
                        }
                    })
                    .build();
            beanProcessor.process();
        }
    }

    private Index buildApplicationIndex() throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> appClasses = Files.walk(deploymentDir.appClasses)) {
            List<Path> classFiles = appClasses.filter(it -> it.toString().endsWith(".class")).collect(Collectors.toList());
            for (Path classFile : classFiles) {
                try (InputStream in = Files.newInputStream(classFile)) {
                    indexer.index(in);
                }
            }
        }
        try (Stream<Path> appLibraries = Files.walk(deploymentDir.appLibraries)) {
            List<Path> jarFiles = appLibraries.filter(it -> it.toString().endsWith(".jar")).collect(Collectors.toList());
            for (Path jarFile : jarFiles) {
                try (JarFile jar = new JarFile(jarFile.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            try (InputStream in = jar.getInputStream(entry)) {
                                indexer.index(in);
                            }
                        }
                    }
                }
            }
        }
        return indexer.complete();
    }

    private IndexView buildImmutableBeanArchiveIndex(Index applicationIndex, Set<String> additionalClasses) throws IOException {
        Indexer indexer = new Indexer();

        Set<String> seen = new HashSet<>();

        // 1. classes in bean archives
        for (BeanArchive beanArchive : beanArchives) {
            for (String classFile : beanArchive.classes) {
                indexFromTCCL(indexer, classFile);
                seen.add(classFile);
            }
        }

        // 2. additional classes added through build compatible extensions
        for (String additionalClass : additionalClasses) {
            String classFile = additionalClass.replace('.', '/') + ".class";
            if (seen.contains(classFile)) {
                continue;
            }
            indexFromTCCL(indexer, classFile);
            seen.add(classFile);
        }

        // 3. test class
        {
            String classFile = testClass.replace('.', '/') + ".class";
            if (!seen.contains(classFile)) {
                indexFromTCCL(indexer, classFile);
                seen.add(classFile);
            }
        }

        // 4. CDI-related annotations (scopes, qualifiers, interceptor bindings, stereotypes)
        // CDI recognizes them even if they come from an archive that is not a bean archive
        Set<DotName> metaAnnotations = Set.of(DotNames.SCOPE, DotNames.NORMAL_SCOPE, DotNames.QUALIFIER,
                DotNames.INTERCEPTOR_BINDING, DotNames.STEREOTYPE);
        for (DotName metaAnnotation : metaAnnotations) {
            for (AnnotationInstance annotation : applicationIndex.getAnnotations(metaAnnotation)) {
                if (annotation.target().kind().equals(AnnotationTarget.Kind.CLASS)) {
                    String annotationClass = annotation.target().asClass().name().toString();
                    String classFile = annotationClass.replace('.', '/') + ".class";
                    if (seen.contains(classFile)) {
                        continue;
                    }
                    indexFromTCCL(indexer, classFile);
                    seen.add(classFile);
                }
            }
        }

        return BeanArchives.buildImmutableBeanArchiveIndex(indexer.complete());
    }

    private void indexFromTCCL(Indexer indexer, String classFile) throws IOException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = tccl.getResourceAsStream(classFile)) {
            indexer.index(in);
        }
    }

    private Closeable withDeploymentClassLoader() throws IOException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        DeploymentClassLoader newCl = new DeploymentClassLoader(deploymentDir);

        Thread.currentThread().setContextClassLoader(newCl);
        return new Closeable() {
            @Override
            public void close() throws IOException {
                Thread.currentThread().setContextClassLoader(oldCl);
                newCl.close();
            }
        };
    }
}
