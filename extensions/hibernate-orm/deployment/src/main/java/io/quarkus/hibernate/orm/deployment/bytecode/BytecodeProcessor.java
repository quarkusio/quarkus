package io.quarkus.hibernate.orm.deployment.bytecode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.staticmethods.InterceptedStaticMethodsTransformersRegisteredBuildItem;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderConstantDefinitionBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ConstantBootstrapBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.AotJarEnabled;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusClassFileLocator;
import io.quarkus.hibernate.orm.deployment.model.JpaModelIndexBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.panache.hibernate.common.deployment.HibernateEnhancersRegisteredBuildItem;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.pool.TypePool.CacheProvider;
import net.bytebuddy.pool.TypePool.Default.ReaderMode;
import net.bytebuddy.utility.GraalImageCode;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
final class BytecodeProcessor {

    static {
        // configure ByteBuddy for build reproducibility
        // while it looks like the property is related to GraalVM,
        // it actually needs to be set for reproducible builds it NOT using GraalVM
        // see https://github.com/raphw/byte-buddy/commit/9b4690956dd049373a0c9fa548f74a16557cf3c0
        System.setProperty(GraalImageCode.REPRODUCIBLE_PROPERTIES, "true");
    }

    @Consume(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    @BuildStep
    @SuppressWarnings("deprecation")
    public HibernateEnhancersRegisteredBuildItem enhancerDomainObjects(JpaModelBuildItem jpaModel,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        // Modify the bytecode of all entities to enable lazy-loading, dirty checking, etc..
        enhanceEntities(jpaModel, transformers, additionalJpaModelBuildItems,
                deprecatedAdditionalJpaModelBuildItems, additionalClasses);
        // this allows others to register their enhancers after Hibernate, so they run before ours
        return new HibernateEnhancersRegisteredBuildItem();
    }

    @BuildStep
    public BytecodeRecorderConstantDefinitionBuildItem pregenProxies(
            JpaModelBuildItem jpaModel,
            JpaModelIndexBuildItem indexBuildItem,
            TransformedClassesBuildItem transformedClassesBuildItem,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem,
            ExecutorService buildExecutor) throws ExecutionException, InterruptedException {
        Set<String> managedClassAndPackageNames = new HashSet<>(jpaModel.getEntityClassNames());
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            // Note: getManagedClassNames() can also return *package* names
            // See the source code of Hibernate ORM for proof:
            // org.hibernate.boot.archive.scan.internal.ScanResultCollector.isListedOrDetectable
            // is used for packages too, and it relies (indirectly) on getManagedClassNames().
            managedClassAndPackageNames.addAll(pud.getManagedClassNames());
        }

        for (AdditionalJpaModelBuildItem additionalJpaModelBuildItem : additionalJpaModelBuildItems) {
            managedClassAndPackageNames.add(additionalJpaModelBuildItem.getClassName());
        }

        // Remove package names: only actual class names should be passed to generateProxies,
        // because attempting to look up a package name via IndexWrapper.getClassByName()
        // triggers a spurious "Failed to index" warning.
        managedClassAndPackageNames.removeAll(jpaModel.getAllModelPackageNames());

        PreGeneratedProxies proxyDefinitions = generateProxies(managedClassAndPackageNames,
                indexBuildItem.getIndex(), transformedClassesBuildItem,
                generatedClassBuildItemBuildProducer, liveReloadBuildItem, buildExecutor);

        // Make proxies available through a constant;
        // this is a hack to avoid introducing circular dependencies between build steps.
        //
        // If we just passed the proxy definitions to #build as a normal build item,
        // we would have the following dependencies:
        //
        // #pregenProxies => ProxyDefinitionsBuildItem => #build => BeanContainerListenerBuildItem
        // => Arc container init => BeanContainerBuildItem
        // => some RestEasy Reactive Method => BytecodeTransformerBuildItem
        // => build step that transforms bytecode => TransformedClassesBuildItem
        // => #pregenProxies
        //
        // Since the dependency from #preGenProxies to #build is only a static init thing
        // (#build needs to pass the proxy definitions to the recorder),
        // we get rid of the circular dependency by defining a constant
        // to pass the proxy definitions to the recorder.
        // That way, the dependency is only between #pregenProxies
        // and the build step that generates the bytecode of bytecode recorders.
        return new BytecodeRecorderConstantDefinitionBuildItem(PreGeneratedProxies.class, proxyDefinitions);
    }

    @BuildStep
    public void preGenAnnotationProxies(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {
        if (hasXmlMappings(persistenceUnitDescriptorBuildItems)) {
            // XML mapping may need to create annotation proxies, which requires reflection
            // and pre-generation of the proxy classes.
            // This probably could be optimized,
            // but there are plans to make deep changes to XML mapping in ORM (to rely on Jandex directly),
            // so let's not waste our time on optimizations that won't be relevant in a few months.
            List<String> annotationClassNames = new ArrayList<>();
            for (DotName name : ClassNames.JPA_MAPPING_ANNOTATIONS) {
                annotationClassNames.add(name.toString());
            }
            for (DotName name : ClassNames.HIBERNATE_MAPPING_ANNOTATIONS) {
                annotationClassNames.add(name.toString());
            }
            reflective.produce(ReflectiveClassBuildItem.builder(annotationClassNames)
                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                    .methods().fields().build());
            for (String annotationClassName : annotationClassNames) {
                proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(annotationClassName));
            }
        }
    }

    private boolean hasXmlMappings(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems) {
        for (PersistenceUnitDescriptorBuildItem descriptor : persistenceUnitDescriptorBuildItems) {
            if (descriptor.hasXmlMappings()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hibernate ORM checks package-info and if we have a negative lookup, it's not cached by AOT class loading.
     * <p>
     * So point of this method is to generate an empty package-info in packages where we have a mapped class,
     * if there isn't a package-info already.
     */
    @BuildStep(onlyIf = AotJarEnabled.class, onlyIfNot = NativeOrNativeSourcesBuild.class)
    void generateMissingPackageInfos(CombinedIndexBuildItem combinedIndex,
            JpaModelBuildItem jpaModel,
            List<ApplicationClassPredicateBuildItem> predicates,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps) {

        IndexView index = combinedIndex.getIndex();

        Set<String> packages = new HashSet<>();
        for (String entityClass : jpaModel.getManagedClassNames()) {
            int idx = entityClass.lastIndexOf('.');
            if (idx > 0) {
                packages.add(entityClass.substring(0, idx));
            }
        }

        if (packages.isEmpty()) {
            return;
        }

        Predicate<String> appClassPredicate = new Predicate<String>() {
            @Override
            public boolean test(String className) {
                for (ApplicationClassPredicateBuildItem predicate : predicates) {
                    if (predicate.test(className)) {
                        return true;
                    }
                }
                return GeneratedClassGizmo2Adaptor.isApplicationClass(className);
            }
        };

        Gizmo gizmo = Gizmo
                .create(new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources, generatedServiceProviders,
                        constantBootstraps, appClassPredicate))
                .withDebugInfo(false)
                .withParameters(false);

        for (String pkg : packages) {
            String packageInfoClassName = pkg + ".package-info";
            if (index.getClassByName(DotName.createSimple(packageInfoClassName)) != null) {
                // we already have a package-info, we don't generate an empty one
                continue;
            }

            gizmo.interface_(packageInfoClassName, cc -> {
                cc.synthetic();
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void enhanceEntities(final JpaModelBuildItem jpaModel,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : jpaModel.getManagedClassNames()) {

            transformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(i)
                    .setVisitorFunction(hibernateEntityEnhancer)
                    .setCacheable(true).build());
        }
        Set<String> additionalClassNames = new HashSet<>();
        for (AdditionalJpaModelBuildItem additionalJpaModel : additionalJpaModelBuildItems) {
            additionalClassNames.add(additionalJpaModel.getClassName());
        }
        for (io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem additionalJpaModel : deprecatedAdditionalJpaModelBuildItems) {
            additionalClassNames.add(additionalJpaModel.getClassName());
        }
        for (String className : additionalClassNames) {
            try {
                byte[] bytes = IoUtil.readClassAsBytes(BytecodeProcessor.class.getClassLoader(), className);
                if (bytes == null) {
                    bytes = IoUtil.readClassAsBytes(Thread.currentThread().getContextClassLoader(), className);
                }
                if (bytes == null) {
                    throw new RuntimeException("Failed to read class bytes for '" + className
                            + "', class not present in class loaders: "
                            + BytecodeProcessor.class.getClassLoader()
                            + ", " + Thread.currentThread().getContextClassLoader());
                }
                byte[] enhanced = hibernateEntityEnhancer.enhance(className, bytes);
                additionalClasses.produce(new GeneratedClassBuildItem(false, className, enhanced != null ? enhanced : bytes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Model class", e);
            }
        }
    }

    private PreGeneratedProxies generateProxies(Set<String> managedClassAndPackageNames, IndexView combinedIndex,
            TransformedClassesBuildItem transformedClassesBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem,
            ExecutorService buildExecutor) throws ExecutionException, InterruptedException {
        ProxyCache proxyCache = liveReloadBuildItem.getContextObject(ProxyCache.class);
        if (proxyCache == null) {
            proxyCache = new ProxyCache();
            liveReloadBuildItem.setContextObject(ProxyCache.class, proxyCache);
        }
        Set<String> changedClasses = Set.of();
        if (liveReloadBuildItem.getChangeInformation() != null) {
            changedClasses = liveReloadBuildItem.getChangeInformation().getChangedClasses();
        } else {
            //we don't have class change info, invalidate the cache
            proxyCache.cache.clear();
        }
        //create a map of entity to proxy type
        TypePool transformedClassesTypePool = createTransformedClassesTypePool(transformedClassesBuildItem,
                managedClassAndPackageNames);

        PreGeneratedProxies preGeneratedProxies = new PreGeneratedProxies();

        try (ProxyBuildingHelper proxyHelper = new ProxyBuildingHelper(transformedClassesTypePool)) {
            final ConcurrentLinkedDeque<Future<CachedProxy>> generatedProxyQueue = new ConcurrentLinkedDeque<>();
            Set<String> proxyInterfaceNames = Set.of(ClassNames.HIBERNATE_PROXY.toString());

            for (String managedClassOrPackageName : managedClassAndPackageNames) {
                if (proxyCache.cache.containsKey(managedClassOrPackageName)
                        && !isModified(managedClassOrPackageName, changedClasses, combinedIndex)) {
                    CachedProxy proxy = proxyCache.cache.get(managedClassOrPackageName);
                    generatedProxyQueue.add(CompletableFuture.completedFuture(proxy));
                } else {
                    if (!proxyHelper.isProxiable(combinedIndex.getClassByName(managedClassOrPackageName))) {
                        // we need to make sure we have a class and not a package and that it is proxiable
                        continue;
                    }
                    // we now are sure we have a proper class and not a package, let's avoid the confusion
                    String managedClass = managedClassOrPackageName;
                    generatedProxyQueue.add(buildExecutor.submit(() -> {
                        DynamicType.Unloaded<?> unloaded = proxyHelper.buildUnloadedProxy(managedClass);
                        return new CachedProxy(managedClass, unloaded);
                    }));
                }
            }

            for (Future<CachedProxy> proxyFuture : generatedProxyQueue) {
                CachedProxy proxy = proxyFuture.get();
                proxyCache.cache.put(proxy.managedClassName, proxy);
                for (Entry<TypeDescription, byte[]> i : proxy.proxyDef.getAllTypes().entrySet()) {
                    generatedClassBuildItemBuildProducer
                            .produce(new GeneratedClassBuildItem(true, i.getKey().getName(), i.getValue()));
                }
                preGeneratedProxies.getProxies().put(proxy.managedClassName, new PreGeneratedProxies.ProxyClassDetailsHolder(
                        proxy.proxyDef.getTypeDescription().getName()));
            }
        }

        return preGeneratedProxies;
    }

    // Creates a TypePool that is aware of class transformations applied to entity classes,
    // so that ByteBuddy can take these transformations into account.
    // This is especially important when getters/setters are added to entity classes,
    // because we want those methods to be overridden in proxies to trigger proxy initialization.
    private TypePool createTransformedClassesTypePool(TransformedClassesBuildItem transformedClassesBuildItem,
            Set<String> entityClasses) {
        Map<String, byte[]> transformedClasses = new HashMap<>();
        for (Set<TransformedClassesBuildItem.TransformedClass> transformedClassSet : transformedClassesBuildItem
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass transformedClass : transformedClassSet) {
                String className = transformedClass.getClassName();
                if (entityClasses.contains(className)) {
                    transformedClasses.put(className, transformedClass.getData());
                }
            }
        }

        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(
                new ClassFileLocator.Simple(transformedClasses),
                QuarkusClassFileLocator.INSTANCE);

        // we can reuse the core TypePool but we may not reuse the full enhancer TypePool
        // or PublicFieldWithProxyAndLazyLoadingAndInheritanceTest will fail
        return new TypePool.Default(new CacheProvider.Simple(), classFileLocator, ReaderMode.FAST,
                HibernateEntityEnhancer.CORE_TYPE_POOL);
    }

    private boolean isModified(String entity, Set<String> changedClasses, IndexView index) {
        if (changedClasses.contains(entity)) {
            return true;
        }
        ClassInfo clazz = index.getClassByName(DotName.createSimple(entity));
        if (clazz == null) {
            //if it is not in the index, then it has not been modified
            return false;
        }
        for (DotName i : clazz.interfaceNames()) {
            if (isModified(i.toString(), changedClasses, index)) {
                return true;
            }
        }
        DotName superName = clazz.superName();
        if (superName != null && !DotName.OBJECT_NAME.equals(superName)) {
            return isModified(superName.toString(), changedClasses, index);
        }
        return false;
    }

    private static final class ProxyCache {

        Map<String, CachedProxy> cache = new HashMap<>();
    }

    static final class CachedProxy {
        final String managedClassName;
        final DynamicType.Unloaded<?> proxyDef;

        CachedProxy(String managedClassName, DynamicType.Unloaded<?> proxyDef) {
            this.managedClassName = managedClassName;
            this.proxyDef = proxyDef;
        }
    }
}
