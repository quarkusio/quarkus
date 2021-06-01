package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.ClassPathResource;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * The result of the curate step that is done by QuarkusBootstrap.
 *
 * This is responsible creating all the class loaders used by the application.
 *
 *
 */
public class CuratedApplication implements Serializable, AutoCloseable {

    private static final String AUGMENTOR = "io.quarkus.runner.bootstrap.AugmentActionImpl";

    /**
     * The class path elements for the various artifacts. These can be used in multiple class loaders
     * so this map allows them to be shared.
     *
     * This should not be used for hot reloadable elements
     */
    private final Map<AppArtifact, List<ClassPathElement>> augmentationElements = new HashMap<>();
    private final boolean assertionsEnabled;

    /**
     * The augmentation class loader.
     */
    private volatile QuarkusClassLoader augmentClassLoader;

    /**
     * The base runtime class loader.
     */
    private volatile QuarkusClassLoader baseRuntimeClassLoader;

    private final QuarkusBootstrap quarkusBootstrap;
    private final CurationResult curationResult;
    private final ConfiguredClassLoading configuredClassLoading;

    final AppModel appModel;

    CuratedApplication(QuarkusBootstrap quarkusBootstrap, CurationResult curationResult,
            ConfiguredClassLoading configuredClassLoading, boolean assertionsEnabled) {
        this.quarkusBootstrap = quarkusBootstrap;
        this.curationResult = curationResult;
        this.appModel = curationResult.getAppModel();
        this.configuredClassLoading = configuredClassLoading;
        this.assertionsEnabled = assertionsEnabled;
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public QuarkusBootstrap getQuarkusBootstrap() {
        return quarkusBootstrap;
    }

    public boolean hasUpdatedDeps() {
        return curationResult.hasUpdatedDeps();
    }

    public List<AppDependency> getUpdatedDeps() {
        return curationResult.getUpdatedDependencies();
    }

    public Object runInAugmentClassLoader(String consumerName, Map<String, Object> params) {
        return runInCl(consumerName, params, getAugmentClassLoader());
    }

    public CurationResult getCurationResult() {
        return curationResult;
    }

    public AugmentAction createAugmentor() {
        try {
            Class<?> augmentor = getAugmentClassLoader().loadClass(AUGMENTOR);
            return (AugmentAction) augmentor.getConstructor(CuratedApplication.class).newInstance(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This creates an augmentor, but uses the supplied class name to customise the build chain.
     *
     * The class name that is passed in must be the name of an implementation of
     * {@code Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>>}
     * which is used to generate a list of build chain customisers to control the build.
     */
    public AugmentAction createAugmentor(String functionName, Map<String, Object> props) {
        try {
            Class<?> augmentor = getAugmentClassLoader().loadClass(AUGMENTOR);
            Function<Object, List<?>> function = (Function<Object, List<?>>) getAugmentClassLoader().loadClass(functionName)
                    .newInstance();
            List<?> res = function.apply(props);
            return (AugmentAction) augmentor.getConstructor(CuratedApplication.class, List.class).newInstance(this, res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object runInCl(String consumerName, Map<String, Object> params, QuarkusClassLoader cl) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            Class<? extends BiConsumer<CuratedApplication, Map<String, Object>>> clazz = (Class<? extends BiConsumer<CuratedApplication, Map<String, Object>>>) cl
                    .loadClass(consumerName);
            BiConsumer<CuratedApplication, Map<String, Object>> biConsumer = clazz.newInstance();
            biConsumer.accept(this, params);
            return biConsumer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private synchronized void processCpElement(AppArtifact artifact, Consumer<ClassPathElement> consumer) {
        if (!artifact.getType().equals(BootstrapConstants.JAR)) {
            //avoid the need for this sort of check in multiple places
            consumer.accept(ClassPathElement.EMPTY);
            return;
        }
        List<ClassPathElement> cpeList = augmentationElements.get(artifact);
        if (cpeList != null) {
            for (ClassPathElement cpe : cpeList) {
                consumer.accept(cpe);
            }
            return;
        }
        cpeList = new ArrayList<>(2);
        for (Path path : artifact.getPaths()) {
            final ClassPathElement element = ClassPathElement.fromPath(path);
            consumer.accept(element);
            cpeList.add(element);
        }
        augmentationElements.put(artifact, cpeList);
    }

    private void addCpElement(QuarkusClassLoader.Builder builder, AppArtifact dep, ClassPathElement element) {
        final AppArtifactKey key = dep.getKey();
        if (appModel.getParentFirstArtifacts().contains(key)
                || configuredClassLoading.parentFirstArtifacts.contains(dep.getKey())) {
            //we always load this from the parent if it is available, as this acts as a bridge between the running
            //app and the dev mode code
            builder.addParentFirstElement(element);
        } else if (appModel.getLesserPriorityArtifacts().contains(key)) {
            builder.addLesserPriorityElement(element);
        }
        builder.addElement(element);
    }

    public synchronized QuarkusClassLoader getAugmentClassLoader() {
        if (augmentClassLoader == null) {
            //first run, we need to build all the class loaders
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder(
                    "Augmentation Class Loader: " + quarkusBootstrap.getMode(),
                    quarkusBootstrap.getBaseClassLoader(), !quarkusBootstrap.isIsolateDeployment())
                    .setAssertionsEnabled(assertionsEnabled);
            builder.addClassLoaderEventListeners(quarkusBootstrap.getClassLoaderEventListeners());
            //we want a class loader that can load the deployment artifacts and all their dependencies, but not
            //any of the runtime artifacts, or user classes
            //this will load any deployment artifacts from the parent CL if they are present
            for (AppDependency i : appModel.getFullDeploymentDeps()) {
                if (configuredClassLoading.reloadableArtifacts.contains(i.getArtifact().getKey())) {
                    continue;
                }
                processCpElement(i.getArtifact(), element -> addCpElement(builder, i.getArtifact(), element));
            }

            for (Path i : quarkusBootstrap.getAdditionalDeploymentArchives()) {
                builder.addElement(ClassPathElement.fromPath(i));
            }
            augmentClassLoader = builder.build();
        }
        return augmentClassLoader;
    }

    /**
     * creates the base runtime class loader.
     *
     * This does not have any generated resources or transformers, these are added by the startup action.
     *
     * The first thing the startup action needs to do is reset this to include generated resources and transformers,
     * as each startup can generate new resources.
     *
     */
    public synchronized QuarkusClassLoader getBaseRuntimeClassLoader() {
        if (baseRuntimeClassLoader == null) {
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder(
                    "Quarkus Base Runtime ClassLoader: " + quarkusBootstrap.getMode(),
                    quarkusBootstrap.getBaseClassLoader(), false)
                    .setAssertionsEnabled(assertionsEnabled);
            builder.addClassLoaderEventListeners(quarkusBootstrap.getClassLoaderEventListeners());

            if (quarkusBootstrap.getMode() == QuarkusBootstrap.Mode.TEST && quarkusBootstrap.isFlatClassPath()) {
                //in test mode we have everything in the base class loader
                //there is no need to restart so there is no need for an additional CL

                for (Path root : quarkusBootstrap.getApplicationRoot()) {
                    builder.addElement(ClassPathElement.fromPath(root));
                }
            } else {
                for (Path root : quarkusBootstrap.getApplicationRoot()) {
                    builder.addBannedElement(new ClassFilteredBannedElement(ClassPathElement.fromPath(root)));
                }
            }

            //additional user class path elements first
            Set<Path> hotReloadPaths = new HashSet<>();
            for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
                if (!i.isHotReloadable()) {
                    for (Path root : i.getArchivePath()) {
                        builder.addElement(ClassPathElement.fromPath(root));
                    }
                } else {
                    for (Path root : i.getArchivePath()) {
                        hotReloadPaths.add(root);
                        builder.addBannedElement(new ClassFilteredBannedElement(ClassPathElement.fromPath(root)));
                    }
                }
            }
            builder.setResettableElement(new MemoryClassPathElement(Collections.emptyMap()));

            for (AppDependency dependency : appModel.getUserDependencies()) {
                if (isHotReloadable(dependency.getArtifact(), hotReloadPaths)) {
                    continue;
                }
                if (configuredClassLoading.reloadableArtifacts.contains(dependency.getArtifact().getKey())) {
                    continue;
                }
                processCpElement(dependency.getArtifact(), element -> addCpElement(builder, dependency.getArtifact(), element));
            }

            baseRuntimeClassLoader = builder.build();
        }
        return baseRuntimeClassLoader;
    }

    private static boolean isHotReloadable(AppArtifact a, Set<Path> hotReloadPaths) {
        for (Path p : a.getPaths()) {
            if (hotReloadPaths.contains(p)) {
                return true;
            }
        }
        return false;
    }

    public QuarkusClassLoader createDeploymentClassLoader() {
        //first run, we need to build all the class loaders
        QuarkusClassLoader.Builder builder = QuarkusClassLoader
                .builder("Deployment Class Loader: " + quarkusBootstrap.getMode(),
                        getAugmentClassLoader(), false)
                .addClassLoaderEventListeners(quarkusBootstrap.getClassLoaderEventListeners())
                .setAssertionsEnabled(assertionsEnabled)
                .setAggregateParentResources(true);

        for (Path root : quarkusBootstrap.getApplicationRoot()) {
            builder.addElement(ClassPathElement.fromPath(root));
        }

        builder.setResettableElement(new MemoryClassPathElement(Collections.emptyMap()));

        //additional user class path elements first
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            for (Path root : i.getArchivePath()) {
                builder.addElement(ClassPathElement.fromPath(root));
            }
        }
        for (AppDependency dependency : appModel.getUserDependencies()) {
            if (configuredClassLoading.reloadableArtifacts.contains(dependency.getArtifact().getKey())) {
                processCpElement(dependency.getArtifact(), element -> addCpElement(builder, dependency.getArtifact(), element));
            }
        }
        return builder.build();
    }

    public QuarkusClassLoader createRuntimeClassLoader(Map<String, byte[]> resources, Map<String, byte[]> transformedClasses) {
        return createRuntimeClassLoader(getBaseRuntimeClassLoader(), resources, transformedClasses);
    }

    public QuarkusClassLoader createRuntimeClassLoader(ClassLoader base, Map<String, byte[]> resources,
            Map<String, byte[]> transformedClasses) {
        QuarkusClassLoader.Builder builder = QuarkusClassLoader
                .builder("Quarkus Runtime ClassLoader: " + quarkusBootstrap.getMode(),
                        getBaseRuntimeClassLoader(), false)
                .setAssertionsEnabled(assertionsEnabled)
                .setAggregateParentResources(true);
        builder.setTransformedClasses(transformedClasses);

        builder.addElement(new MemoryClassPathElement(resources));
        for (Path root : quarkusBootstrap.getApplicationRoot()) {
            builder.addElement(ClassPathElement.fromPath(root));
        }

        for (AdditionalDependency i : getQuarkusBootstrap().getAdditionalApplicationArchives()) {
            if (i.isHotReloadable()) {
                for (Path root : i.getArchivePath()) {
                    builder.addElement(ClassPathElement.fromPath(root));
                }
            }
        }
        for (AppDependency dependency : appModel.getUserDependencies()) {
            if (configuredClassLoading.reloadableArtifacts.contains(dependency.getArtifact().getKey())) {
                processCpElement(dependency.getArtifact(), element -> addCpElement(builder, dependency.getArtifact(), element));
            }
        }
        return builder.build();
    }

    @Override
    public void close() {
        if (augmentClassLoader != null) {
            augmentClassLoader.close();
        }
        if (baseRuntimeClassLoader != null) {
            baseRuntimeClassLoader.close();
        }
    }

    /**
     * TODO: Fix everything in the universe to do loading properly
     * 
     * This class exists because a lot of libraries do getClass().getClassLoader.getResource()
     * instead of using the context class loader, which breaks tests as these resources are present in the
     * top CL and not the base CL that is used to load libraries.
     * 
     * This yucky yucky hack works around this, by allowing non-class files to be loaded parent first, so they
     * will be loaded from the application ClassLoader.
     *
     * Note that the underlying reason for this 'banned element' existing in the first place
     * is because other libraries do Class Loading wrong in different ways, and attempt to load
     * from the TCCL as a fallback instead of as the first priority, so we need to have the banned element
     * to prevent a load from the application ClassLoader (which won't work).
     *
     */
    static class ClassFilteredBannedElement implements ClassPathElement {

        private final ClassPathElement delegate;

        ClassFilteredBannedElement(ClassPathElement delegate) {
            this.delegate = delegate;
        }

        @Override
        public Path getRoot() {
            return delegate.getRoot();
        }

        @Override
        public ClassPathResource getResource(String name) {
            if (!name.endsWith(".class")) {
                return null;
            }
            return delegate.getResource(name);
        }

        @Override
        public Set<String> getProvidedResources() {
            return delegate.getProvidedResources().stream().filter(s -> s.endsWith(".class")).collect(Collectors.toSet());
        }

        @Override
        public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
            return delegate.getProtectionDomain(classLoader);
        }

        @Override
        public Manifest getManifest() {
            return delegate.getManifest();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

}
