package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import java.io.Closeable;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.objectweb.asm.ClassVisitor;

/**
 * The result of the curate step that is done by QuarkusBootstrap.
 *
 * This is responsible creating all the class loaders used by the application.
 *
 *
 */
public class CuratedApplication implements Serializable, Closeable {

    private static final String AUGMENTOR = "io.quarkus.runner.bootstrap.AugmentActionImpl";

    /**
     * The class path elements for the various artifacts. These can be used in multiple class loaders
     * so this map allows them to be shared.
     *
     * This should not be used for hot reloadable elements
     */
    private final Map<AppArtifact, ClassPathElement> augmentationElements = new HashMap<>();

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
    final AppModel appModel;

    CuratedApplication(QuarkusBootstrap quarkusBootstrap, CurationResult curationResult) {
        this.quarkusBootstrap = quarkusBootstrap;
        this.curationResult = curationResult;
        this.appModel = curationResult.getAppModel();
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

    public Object runInBaseRuntimeClassLoader(String consumerName, Map<String, Object> params) {
        return runInCl(consumerName, params, getBaseRuntimeClassLoader());
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
        if (augmentationElements.containsKey(artifact)) {
            consumer.accept(augmentationElements.get(artifact));
            return;
        }
        artifact.getPaths().forEach(path -> {
            final ClassPathElement element = ClassPathElement.fromPath(path);
            augmentationElements.put(artifact, element);
            consumer.accept(element);
        });
    }

    private void addCpElement(QuarkusClassLoader.Builder builder, AppArtifact dep, ClassPathElement element) {
        final AppArtifactKey key = dep.getKey();
        if (appModel.getParentFirstArtifacts().contains(key)) {
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
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Augmentation Class Loader",
                    quarkusBootstrap.getBaseClassLoader(), !quarkusBootstrap.isIsolateDeployment());
            //we want a class loader that can load the deployment artifacts and all their dependencies, but not
            //any of the runtime artifacts, or user classes
            //this will load any deployment artifacts from the parent CL if they are present
            for (AppDependency i : appModel.getFullDeploymentDeps()) {
                processCpElement(i.getArtifact(), element -> addCpElement(builder, i.getArtifact(), element));
            }

            processCpElement(appModel.getAppArtifact(), element -> addCpElement(builder, appModel.getAppArtifact(), element));

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
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Quarkus Base Runtime ClassLoader",
                    quarkusBootstrap.getBaseClassLoader(), false);
            if (quarkusBootstrap.getMode() == QuarkusBootstrap.Mode.TEST) {
                //in test mode we have everything in the base class loader
                //there is no need to restart so there is no need for an additional CL

                for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
                    //src/test is the highest priority
                    if (i.isTestClassRoot()) {
                        builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
                    }
                }

                if (quarkusBootstrap.getApplicationRoot() != null) {
                    builder.addElement(ClassPathElement.fromPath(quarkusBootstrap.getApplicationRoot()));
                }
            }
            //additional user class path elements first
            Set<Path> hotReloadPaths = new HashSet<>();
            for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
                if (!i.isTestClassRoot()) {
                    if (!i.isHotReloadable()) {
                        builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
                    } else {
                        hotReloadPaths.add(i.getArchivePath());
                    }
                }
            }
            builder.setResettableElement(new MemoryClassPathElement(Collections.emptyMap()));

            for (AppDependency dependency : appModel.getUserDependencies()) {
                if (isHotReloadable(dependency.getArtifact(), hotReloadPaths)) {
                    continue;
                }
                processCpElement(dependency.getArtifact(), element -> addCpElement(builder, dependency.getArtifact(), element));
            }

            processCpElement(appModel.getAppArtifact(), element -> addCpElement(builder, appModel.getAppArtifact(), element));

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
        QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Deployment Class Loader",
                getAugmentClassLoader(), false)
                .setAggregateParentResources(true);
        //add the application root, and test roots
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            if (i.isTestClassRoot()) {
                builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
            }
        }

        if (quarkusBootstrap.getApplicationRoot() != null) {
            builder.addElement(ClassPathElement.fromPath(quarkusBootstrap.getApplicationRoot()));
        }

        //additional user class path elements first
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            if (!i.isTestClassRoot()) {
                builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
            }
        }
        return builder.build();
    }

    public QuarkusClassLoader createRuntimeClassLoader(QuarkusClassLoader loader,
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers,
            ClassLoader deploymentClassLoader, Map<String, byte[]> resources) {
        QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Quarkus Runtime ClassLoader",
                loader, false)
                .setAggregateParentResources(true);
        builder.setTransformerClassLoader(deploymentClassLoader);
        if (quarkusBootstrap.getApplicationRoot() != null) {
            builder.addElement(ClassPathElement.fromPath(quarkusBootstrap.getApplicationRoot()));
        }
        builder.addElement(new MemoryClassPathElement(resources));

        for (AdditionalDependency i : getQuarkusBootstrap().getAdditionalApplicationArchives()) {
            if (i.isHotReloadable()) {
                builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
            }
        }
        builder.setBytecodeTransformers(bytecodeTransformers);
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
}
