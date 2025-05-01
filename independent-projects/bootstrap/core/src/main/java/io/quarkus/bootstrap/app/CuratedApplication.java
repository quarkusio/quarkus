package io.quarkus.bootstrap.app;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.ClassPathResource;
import io.quarkus.bootstrap.classloading.FilteredClassPathElement;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.ManifestAttributes;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;

/**
 * The result of the curate step that is done by QuarkusBootstrap.
 *
 * This is responsible creating all the class loaders used by the application.
 *
 *
 */
public class CuratedApplication implements Serializable, AutoCloseable {

    private static final long serialVersionUID = 7816596453653911149L;

    private static final String AUGMENTOR = "io.quarkus.runner.bootstrap.AugmentActionImpl";

    /**
     * The class path elements for the various artifacts. These can be used in multiple class loaders
     * so this map allows them to be shared.
     *
     * This should not be used for hot reloadable elements
     */
    private final Map<ArtifactKey, ClassPathElement> augmentationElements = new HashMap<>();

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

    final ApplicationModel appModel;

    final AtomicInteger runtimeClassLoaderCount = new AtomicInteger();
    private boolean eligibleForReuse = false;

    CuratedApplication(QuarkusBootstrap quarkusBootstrap, CurationResult curationResult,
            ConfiguredClassLoading configuredClassLoading) {
        this.quarkusBootstrap = quarkusBootstrap;
        this.curationResult = curationResult;
        this.appModel = curationResult.getApplicationModel();
        this.configuredClassLoading = configuredClassLoading;
    }

    public void setEligibleForReuse(boolean eligible) {
        this.eligibleForReuse = eligible;
    }

    public boolean isFlatClassPath() {
        return configuredClassLoading.isFlatTestClassPath();
    }

    public ApplicationModel getApplicationModel() {
        return appModel;
    }

    public QuarkusBootstrap getQuarkusBootstrap() {
        return quarkusBootstrap;
    }

    public Object runInAugmentClassLoader(String consumerName, Map<String, Object> params) {
        return runInCl(consumerName, params, getOrCreateAugmentClassLoader());
    }

    public CurationResult getCurationResult() {
        return curationResult;
    }

    public AugmentAction createAugmentor() {
        try {
            Class<?> augmentor = getOrCreateAugmentClassLoader().loadClass(AUGMENTOR);
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
            Class<?> augmentor = getOrCreateAugmentClassLoader().loadClass(AUGMENTOR);
            Function<Object, List<?>> function = (Function<Object, List<?>>) getOrCreateAugmentClassLoader()
                    .loadClass(functionName)
                    .getDeclaredConstructor()
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
            BiConsumer<CuratedApplication, Map<String, Object>> biConsumer = clazz.getDeclaredConstructor().newInstance();
            biConsumer.accept(this, params);
            return biConsumer;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    /**
     * Turns a resolved dependency into a classpath element and calls a consumer to process it.
     * The consumer will add the classpath element to a {@link QuarkusClassLoader} instance.
     *
     * TODO: useCpeCache argument is a quick workaround to avoid the risk of sharing classpath elements
     * among multiple instances of deployment and runtime classloaders. When these shared classpath elements
     * happen to be JARs, when the first deployment classloader is closed, its JAR classpath elements will
     * get closed as well but will remain cached, so the next deployment classloader will be initialized
     * with the closed JAR classpath elements.
     *
     * @param artifact resolved dependency
     * @param consumer classpath element consumer
     * @param useCpeCache whether to re-use cached classpath elements
     */
    private synchronized void processCpElement(ResolvedDependency artifact, Consumer<ClassPathElement> consumer,
            boolean useCpeCache) {
        if (!artifact.isJar()) {
            //avoid the need for this sort of check in multiple places
            consumer.accept(ClassPathElement.EMPTY);
            return;
        }
        Collection<String> filteredResources = configuredClassLoading.getRemovedResources().get(artifact.getKey());
        if (filteredResources != null) {
            Consumer<ClassPathElement> old = consumer;
            consumer = new Consumer<ClassPathElement>() {
                @Override
                public void accept(ClassPathElement classPathElement) {
                    old.accept(new FilteredClassPathElement(classPathElement, filteredResources));
                }
            };
        }
        ClassPathElement cpe = useCpeCache ? augmentationElements.get(artifact.getKey()) : null;
        if (cpe != null) {
            consumer.accept(cpe);
            return;
        }
        final PathTree contentTree = artifact.getContentTree();
        if (contentTree.isEmpty()) {
            consumer.accept(ClassPathElement.EMPTY);
            return;
        }
        cpe = ClassPathElement.fromDependency(contentTree, artifact);
        consumer.accept(cpe);
        if (useCpeCache) {
            augmentationElements.put(artifact.getKey(), cpe);
        }
    }

    private void addCpElement(QuarkusClassLoader.Builder builder, ResolvedDependency dep, ClassPathElement element) {
        final ArtifactKey key = dep.getKey();
        if (configuredClassLoading.isParentFirstArtifact(key)) {
            //we always load this from the parent if it is available, as this acts as a bridge between the running
            //app and the dev mode code
            builder.addParentFirstElement(element);
            builder.addNormalPriorityElement(element);
        } else if (dep.isFlagSet(DependencyFlags.CLASSLOADER_LESSER_PRIORITY)) {
            builder.addLesserPriorityElement(element);
        } else {
            builder.addNormalPriorityElement(element);
        }
    }

    public synchronized QuarkusClassLoader getOrCreateAugmentClassLoader() {
        if (augmentClassLoader == null) {
            //first run, we need to build all the class loaders
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder(
                    "Augmentation Class Loader: " + quarkusBootstrap.getMode() + getClassLoaderNameSuffix(),
                    quarkusBootstrap.getBaseClassLoader(), !quarkusBootstrap.isIsolateDeployment())
                    .setAssertionsEnabled(quarkusBootstrap.isAssertionsEnabled());
            builder.addClassLoaderEventListeners(quarkusBootstrap.getClassLoaderEventListeners());
            //we want a class loader that can load the deployment artifacts and all their dependencies, but not
            //any of the runtime artifacts, or user classes
            //this will load any deployment artifacts from the parent CL if they are present
            for (ResolvedDependency i : appModel.getDependencies()) {
                if (configuredClassLoading.isRemovedArtifact(i.getKey())) {
                    processCpElement(i, builder::addBannedElement, true);
                    continue;
                }
                if (configuredClassLoading.isReloadableArtifact(i.getKey())) {
                    continue;
                }
                processCpElement(i, element -> addCpElement(builder, i, element), true);
            }

            for (Path i : quarkusBootstrap.getAdditionalDeploymentArchives()) {
                builder.addNormalPriorityElement(ClassPathElement.fromPath(i, false));
            }
            Map<String, byte[]> banned = new HashMap<>();
            for (Collection<String> i : configuredClassLoading.getRemovedResources().values()) {
                for (String j : i) {
                    banned.put(j, new byte[0]);
                }
            }
            builder.addBannedElement(new MemoryClassPathElement(banned, false));
            augmentClassLoader = builder.build();
        }
        return augmentClassLoader;
    }

    /**
     * In most cases {@link #getOrCreateAugmentClassLoader()} should be used but this can be useful if you want to be able to
     * get this instance without creating it (and so potentially get null if it doesn't exist).
     */
    public QuarkusClassLoader getAugmentClassLoader() {
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
    public synchronized QuarkusClassLoader getOrCreateBaseRuntimeClassLoader() {
        if (baseRuntimeClassLoader == null) {
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder(
                    "Quarkus Base Runtime ClassLoader: " + quarkusBootstrap.getMode() + getClassLoaderNameSuffix(),
                    quarkusBootstrap.getBaseClassLoader(), false)
                    .setAssertionsEnabled(quarkusBootstrap.isAssertionsEnabled());
            builder.addClassLoaderEventListeners(quarkusBootstrap.getClassLoaderEventListeners());
            builder.setCuratedApplication(this);

            if (configuredClassLoading.isFlatTestClassPath()) {
                //in test mode we have everything in the base class loader
                //there is no need to restart so there is no need for an additional CL

                for (Path root : quarkusBootstrap.getApplicationRoot()) {
                    builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
                }
            } else {
                for (Path root : quarkusBootstrap.getApplicationRoot()) {
                    builder.addBannedElement(new ClassFilteredBannedElement(ClassPathElement.fromPath(root, true)));
                }
            }

            //additional user class path elements first
            Set<Path> hotReloadPaths = new HashSet<>();
            for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
                if (!i.isHotReloadable()) {
                    for (Path root : i.getResolvedPaths()) {
                        builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
                    }
                } else {
                    for (Path root : i.getResolvedPaths()) {
                        hotReloadPaths.add(root);
                        builder.addBannedElement(new ClassFilteredBannedElement(ClassPathElement.fromPath(root, true)));
                    }
                }
            }
            for (Path i : configuredClassLoading.getAdditionalClasspathElements()) {
                hotReloadPaths.add(i);
                builder.addBannedElement(new ClassFilteredBannedElement(ClassPathElement.fromPath(i, true)));
            }

            builder.setResettableElement(new MemoryClassPathElement(Collections.emptyMap(), true));
            Map<String, byte[]> banned = new HashMap<>();
            for (Collection<String> i : configuredClassLoading.getRemovedResources().values()) {
                for (String j : i) {
                    banned.put(j, new byte[0]);
                }
            }
            builder.addBannedElement(new MemoryClassPathElement(banned, true));

            for (ResolvedDependency dependency : appModel.getDependencies()) {
                if (configuredClassLoading.isRemovedArtifact(dependency.getKey())) {
                    processCpElement(dependency, builder::addBannedElement, true);
                    continue;
                }
                if (!dependency.isRuntimeCp()
                        || isHotReloadable(dependency, hotReloadPaths)
                        || configuredClassLoading.isReloadableArtifact(dependency.getKey())
                        || !configuredClassLoading.isFlatTestClassPath() && dependency.isReloadable()
                                && appModel.getReloadableWorkspaceDependencies().contains(dependency.getKey())) {
                    continue;
                }
                processCpElement(dependency, element -> addCpElement(builder, dependency, element), true);
            }

            baseRuntimeClassLoader = builder.build();
        }
        return baseRuntimeClassLoader;
    }

    /**
     * In most cases {@link #getOrCreateBaseRuntimeClassLoader()} should be used but this can be useful if you want to be able
     * to get this instance without creating it (and so potentially get null if it doesn't exist).
     */
    public QuarkusClassLoader getBaseRuntimeClassLoader() {
        return baseRuntimeClassLoader;
    }

    private static boolean isHotReloadable(ResolvedDependency a, Set<Path> hotReloadPaths) {
        for (Path p : a.getContentTree().getRoots()) {
            if (hotReloadPaths.contains(p)) {
                return true;
            }
        }
        return false;
    }

    public QuarkusClassLoader createDeploymentClassLoader() {
        //first run, we need to build all the class loaders
        QuarkusClassLoader.Builder builder = QuarkusClassLoader
                .builder("Deployment Class Loader: " + quarkusBootstrap.getMode() + getClassLoaderNameSuffix(),
                        getOrCreateAugmentClassLoader(), false)
                .addClassLoaderEventListeners(quarkusBootstrap.getClassLoaderEventListeners())
                .setAssertionsEnabled(quarkusBootstrap.isAssertionsEnabled())
                .setAggregateParentResources(true);

        for (Path root : quarkusBootstrap.getApplicationRoot()) {
            builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
        }

        builder.setResettableElement(new MemoryClassPathElement(Collections.emptyMap(), false));

        //additional user class path elements first
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            for (Path root : i.getResolvedPaths()) {
                builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
            }
        }
        for (ResolvedDependency dependency : appModel.getDependencies()) {
            if (configuredClassLoading.isRemovedArtifact(dependency.getKey())) {
                continue;
            }
            if (isReloadableRuntimeDependency(dependency)) {
                processCpElement(dependency, element -> addCpElement(builder, dependency, element), false);
            }
        }
        for (Path root : configuredClassLoading.getAdditionalClasspathElements()) {
            builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
        }
        return builder.build();
    }

    private boolean isReloadableRuntimeDependency(ResolvedDependency dependency) {
        return dependency.isRuntimeCp() && dependency.isJar() &&
                (dependency.isReloadable() && appModel.getReloadableWorkspaceDependencies().contains(dependency.getKey()) ||
                        configuredClassLoading.isReloadableArtifact(dependency.getKey()));
    }

    public String getClassLoaderNameSuffix() {
        return quarkusBootstrap.getBaseName() != null ? " for " + quarkusBootstrap.getBaseName() : "";
    }

    public QuarkusClassLoader createRuntimeClassLoader(Map<String, byte[]> resources, Map<String, byte[]> transformedClasses) {
        return createRuntimeClassLoader(getOrCreateBaseRuntimeClassLoader(), resources, transformedClasses);
    }

    public QuarkusClassLoader createRuntimeClassLoader(ClassLoader base, Map<String, byte[]> resources,
            Map<String, byte[]> transformedClasses) {
        QuarkusClassLoader.Builder builder = QuarkusClassLoader
                .builder(
                        "Quarkus Runtime ClassLoader: " + quarkusBootstrap.getMode()
                                + getClassLoaderNameSuffix()
                                + " restart no:"
                                + runtimeClassLoaderCount.getAndIncrement(),
                        base, false)
                .setAssertionsEnabled(quarkusBootstrap.isAssertionsEnabled())
                .setCuratedApplication(this)
                .setAggregateParentResources(true);
        builder.setTransformedClasses(transformedClasses);

        builder.addNormalPriorityElement(new MemoryClassPathElement(resources, true));
        for (Path root : quarkusBootstrap.getApplicationRoot()) {
            builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
        }

        for (AdditionalDependency i : getQuarkusBootstrap().getAdditionalApplicationArchives()) {
            if (i.isHotReloadable()) {
                for (Path root : i.getResolvedPaths()) {
                    builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
                }
            }
        }
        for (ResolvedDependency dependency : appModel.getDependencies()) {
            if (configuredClassLoading.isRemovedArtifact(dependency.getKey())) {
                continue;
            }
            if (isReloadableRuntimeDependency(dependency)) {
                processCpElement(dependency, element -> addCpElement(builder, dependency, element), false);
            }
        }
        for (Path root : configuredClassLoading.getAdditionalClasspathElements()) {
            builder.addNormalPriorityElement(ClassPathElement.fromPath(root, true));
        }
        return builder.build();
    }

    public boolean isReloadableArtifact(ArtifactKey key) {
        return this.configuredClassLoading.isReloadableArtifact(key);
    }

    public boolean hasReloadableArtifacts() {
        return this.configuredClassLoading.hasReloadableArtifacts();
    }

    @Override
    public void close() {
        if (augmentClassLoader != null) {
            augmentClassLoader.close();
            augmentClassLoader = null;
        }
        if (baseRuntimeClassLoader != null) {
            baseRuntimeClassLoader.close();
            baseRuntimeClassLoader = null;
        }
        augmentationElements.clear();
    }

    public boolean isEligibleForReuse() {
        return eligibleForReuse;
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
        public ResolvedDependency getResolvedDependency() {
            return delegate.getResolvedDependency();
        }

        @Override
        public boolean isRuntime() {
            return delegate.isRuntime();
        }

        @Override
        public <T> T apply(Function<OpenPathTree, T> func) {
            return delegate.apply(func);
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
        public boolean containsReloadableResources() {
            return delegate.containsReloadableResources();
        }

        @Override
        public ProtectionDomain getProtectionDomain() {
            return delegate.getProtectionDomain();
        }

        @Override
        public ManifestAttributes getManifestAttributes() {
            return delegate.getManifestAttributes();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

}
