package org.jboss.resteasy.reactive.server.processor;

import jakarta.ws.rs.core.Application;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.SerializerScanningResult;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactory;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectiveContextInjectedBeanFactory;
import org.jboss.resteasy.reactive.server.core.startup.RuntimeDeploymentManager;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.processor.scanning.FeatureScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveContextResolverScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveExceptionMappingScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveFeatureScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveParamConverterScanner;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClass;
import org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;
import org.objectweb.asm.ClassVisitor;

/**
 * Class that hides some complexity of assembling a RESTEasy Reactive application.
 * <p>
 * Quarkus does not use this class directly, as it assembles the application itself.
 */
public class ResteasyReactiveDeploymentManager {
    private static final Logger log = Logger.getLogger(ResteasyReactiveDeploymentManager.class);

    public static ScanStep start(IndexView nonCalculatingIndex) {
        return new ScanStep(nonCalculatingIndex);
    }

    public static class ScanStep {
        final IndexView index;
        int inputBufferSize = 10000;
        int outputBufferSize = 8192;
        /**
         * By default, we assume a default produced media type of "text/plain"
         * for String endpoint return types. If this is disabled, the default
         * produced media type will be "[text/plain, *&sol;*]" which is more
         * expensive due to negotiation.
         */
        private boolean singleDefaultProduces;

        /**
         * When one of the quarkus-resteasy-reactive-jackson or quarkus-resteasy-reactive-jsonb extension are active
         * and the result type of an endpoint is an application class or one of {@code Collection}, {@code List}, {@code Set} or
         * {@code Map}, we assume the default return type is "application/json".
         */
        private boolean defaultProduces;

        private Map<DotName, ClassInfo> additionalResources = new HashMap<>();
        private Map<DotName, String> additionalResourcePaths = new HashMap<>();
        private Set<String> excludedClasses = new HashSet<>();
        private Set<DotName> contextTypes = new HashSet<>();
        private String applicationPath;
        private final List<MethodScanner> methodScanners = new ArrayList<>();
        private final List<FeatureScanner> featureScanners = new ArrayList<>();
        private final List<AnnotationsTransformer> annotationsTransformers = new ArrayList<>();

        public ScanStep(IndexView nonCalculatingIndex) {
            index = JandexUtil.createCalculatingIndex(nonCalculatingIndex);
            //we force the indexing of some internal classes
            //so we can correctly detect their inheritors
            index.getClassByName(ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_READER);
            index.getClassByName(ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_WRITER_ALL_WRITER);
            index.getClassByName(ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_WRITER);
        }

        public int getInputBufferSize() {
            return inputBufferSize;
        }

        public ScanStep setInputBufferSize(int inputBufferSize) {
            this.inputBufferSize = inputBufferSize;
            return this;
        }

        public boolean isSingleDefaultProduces() {
            return singleDefaultProduces;
        }

        public ScanStep setSingleDefaultProduces(boolean singleDefaultProduces) {
            this.singleDefaultProduces = singleDefaultProduces;
            return this;
        }

        public ScanStep addContextType(DotName type) {
            contextTypes.add(type);
            return this;
        }

        public ScanStep addContextTypes(Collection<DotName> types) {
            contextTypes.addAll(types);
            return this;
        }

        public boolean isDefaultProduces() {
            return defaultProduces;
        }

        public ScanStep setDefaultProduces(boolean defaultProduces) {
            this.defaultProduces = defaultProduces;
            return this;
        }

        public ScanStep addAdditionalResource(DotName className, ClassInfo classInfo) {
            additionalResources.put(className, classInfo);
            return this;
        }

        public ScanStep addAdditionalResourcePath(DotName className, String path) {
            additionalResourcePaths.put(className, path);
            return this;
        }

        public ScanStep addMethodScanner(MethodScanner methodScanner) {
            this.methodScanners.add(methodScanner);
            return this;
        }

        public ScanStep addFeatureScanner(FeatureScanner methodScanner) {
            this.featureScanners.add(methodScanner);
            return this;
        }

        public ScanStep addAnnotationsTransformer(AnnotationsTransformer annotationsTransformer) {
            this.annotationsTransformers.add(annotationsTransformer);
            return this;
        }

        public String getApplicationPath() {
            return applicationPath;
        }

        public ScanStep setApplicationPath(String applicationPath) {
            this.applicationPath = applicationPath;
            return this;
        }

        public ScanResult scan() {

            ApplicationScanningResult applicationScanningResult = ResteasyReactiveScanner.scanForApplicationClass(index,
                    excludedClasses);
            ResourceScanningResult resources = ResteasyReactiveScanner.scanResources(index, additionalResources,
                    additionalResourcePaths);
            SerializerScanningResult serializerScanningResult = ResteasyReactiveScanner.scanForSerializers(index,
                    applicationScanningResult);

            AdditionalReaders readers = new AdditionalReaders();
            AdditionalWriters writers = new AdditionalWriters();

            List<ResourceClass> resourceClasses = new ArrayList<>();
            List<ResourceClass> subResourceClasses = new ArrayList<>();

            ServerEndpointIndexer.Builder builder = new ServerEndpointIndexer.Builder()
                    .setIndex(index)
                    .setApplicationIndex(index)
                    .addContextTypes(contextTypes)
                    .setAnnotationsTransformers(annotationsTransformers)
                    .setScannedResourcePaths(resources.getScannedResourcePaths())
                    .setClassLevelExceptionMappers(new HashMap<>())
                    .setAdditionalReaders(readers)
                    .setAdditionalWriters(writers)
                    .setInjectableBeans(new HashMap<>())
                    .setConfig(new ResteasyReactiveConfig(inputBufferSize, outputBufferSize, singleDefaultProduces,
                            defaultProduces))
                    .setHttpAnnotationToMethod(resources.getHttpAnnotationToMethod())
                    .setApplicationScanningResult(applicationScanningResult);
            for (MethodScanner scanner : methodScanners) {
                builder.addMethodScanner(scanner);
            }
            for (var i : featureScanners) {
                i.integrateWithIndexer(builder, index);
            }

            ServerEndpointIndexer serverEndpointIndexer = builder
                    .build();
            for (Map.Entry<DotName, ClassInfo> i : resources.getScannedResources().entrySet()) {
                Optional<ResourceClass> res = serverEndpointIndexer.createEndpoints(i.getValue(), true);
                if (res.isPresent()) {
                    resourceClasses.add(res.get());
                }
            }
            for (Map.Entry<DotName, ClassInfo> i : resources.getPossibleSubResources().entrySet()) {
                Optional<ResourceClass> res = serverEndpointIndexer.createEndpoints(i.getValue(), false);
                if (res.isPresent()) {
                    subResourceClasses.add(res.get());
                }
            }

            Features scannedFeatures = ResteasyReactiveFeatureScanner.createFeatures(index, applicationScanningResult);
            ResourceInterceptors resourceInterceptors = ResteasyReactiveInterceptorScanner
                    .createResourceInterceptors(index, applicationScanningResult);
            DynamicFeatures dynamicFeatures = ResteasyReactiveFeatureScanner.createDynamicFeatures(index,
                    applicationScanningResult);
            ParamConverterProviders paramConverters = ResteasyReactiveParamConverterScanner
                    .createParamConverters(index, applicationScanningResult);
            ExceptionMapping exceptionMappers = ResteasyReactiveExceptionMappingScanner.createExceptionMappers(index,
                    applicationScanningResult);
            ContextResolvers contextResolvers = ResteasyReactiveContextResolverScanner.createContextResolvers(index,
                    applicationScanningResult);
            ScannedApplication scannedApplication = new ScannedApplication(resources, readers, writers,
                    serializerScanningResult,
                    applicationScanningResult, resourceClasses, subResourceClasses, scannedFeatures, resourceInterceptors,
                    dynamicFeatures, paramConverters, exceptionMappers, contextResolvers);

            List<GeneratedClass> generatedClasses = new ArrayList<>();
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers = new HashMap<>();
            for (var i : featureScanners) {
                FeatureScanner.FeatureScanResult scanResult = i.integrate(index, scannedApplication);
                generatedClasses.addAll(scanResult.getGeneratedClasses());
                for (var entry : scanResult.getTransformers().entrySet()) {
                    transformers.computeIfAbsent(entry.getKey(), (k) -> new ArrayList<>()).addAll(entry.getValue());
                }
            }
            return new ScanResult(this, scannedApplication, generatedClasses, transformers);
        }

    }

    public static class ScanResult {
        final ScanStep scanStep;
        final ScannedApplication scannedApplication;
        final List<GeneratedClass> generatedClasses;
        final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers;

        ScanResult(ScanStep scanStep, ScannedApplication scannedApplication, List<GeneratedClass> generatedClasses,
                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers) {
            this.scanStep = scanStep;
            this.scannedApplication = scannedApplication;
            this.generatedClasses = generatedClasses;
            this.transformers = transformers;
        }

        public List<GeneratedClass> getGeneratedClasses() {
            return generatedClasses;
        }

        public Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> getTransformers() {
            return transformers;
        }

        public PreparedApplication prepare(ClassLoader loader, Function<String, BeanFactory<?>> factoryCreator) {
            return new PreparedApplication(loader, scanStep, scannedApplication, factoryCreator);
        }
    }

    public static class PreparedApplication {

        final ClassLoader classLoader;
        final ScanStep scanStep;
        final ScannedApplication sa;
        final ServerSerialisers serialisers = new ServerSerialisers();
        final Function<String, BeanFactory<?>> factoryCreator;

        PreparedApplication(ClassLoader classLoader, ScanStep scanStep, ScannedApplication sa,
                Function<String, BeanFactory<?>> factoryCreator) {
            this.classLoader = classLoader;
            this.scanStep = scanStep;
            this.sa = sa;
            this.factoryCreator = factoryCreator;
        }

        public void addScannedSerializers() throws ClassNotFoundException {
            for (var i : sa.serializerScanningResult.getWriters()) {
                serialisers.addWriter(classLoader.loadClass(i.getHandledClassName()),
                        new ResourceWriter()
                                .setMediaTypeStrings(i.getMediaTypeStrings())
                                .setConstraint(i.getRuntimeType())
                                .setBuiltin(i.isBuiltin())
                                .setPriority(i.getPriority())
                                .setFactory(new ReflectionBeanFactory<>(i.getClassName())));
            }
            for (var i : sa.serializerScanningResult.getReaders()) {
                serialisers.addReader(classLoader.loadClass(i.getHandledClassName()),
                        new ResourceReader()
                                .setMediaTypeStrings(i.getMediaTypeStrings())
                                .setConstraint(i.getRuntimeType())
                                .setBuiltin(i.isBuiltin())
                                .setPriority(i.getPriority())
                                .setFactory(new ReflectionBeanFactory<>(i.getClassName())));
            }
            for (var i : sa.writers.get()) {
                serialisers.addWriter(classLoader.loadClass(i.getEntityClass()),
                        new ResourceWriter().setFactory(ReflectiveContextInjectedBeanFactory.create(i.getHandlerClass()))
                                .setConstraint(i.getConstraint())
                                .setMediaTypeStrings(Collections.singletonList(i.getMediaType())));
            }
            for (var i : sa.readers.get()) {
                serialisers.addReader(classLoader.loadClass(i.getEntityClass()),
                        new ResourceReader().setFactory(ReflectiveContextInjectedBeanFactory.create((i.getHandlerClass())))
                                .setConstraint(i.getConstraint())
                                .setMediaTypeStrings(Collections.singletonList(i.getMediaType())));
            }
        }

        public void addBuiltinSerializers() {
            for (Serialisers.BuiltinReader builtinReader : ServerSerialisers.BUILTIN_READERS) {
                serialisers.addReader(builtinReader.entityClass,
                        new ResourceReader().setFactory(ReflectiveContextInjectedBeanFactory.create(builtinReader.readerClass))
                                .setConstraint(builtinReader.constraint)
                                .setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType)).setBuiltin(true));
            }
            for (Serialisers.BuiltinWriter builtinReader : ServerSerialisers.BUILTIN_WRITERS) {
                serialisers.addWriter(builtinReader.entityClass,
                        new ResourceWriter().setFactory(ReflectiveContextInjectedBeanFactory.create(builtinReader.writerClass))
                                .setConstraint(builtinReader.constraint)
                                .setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType)).setBuiltin(true));
            }
        }

        public RunnableApplication createApplication(RuntimeConfiguration runtimeConfiguration,
                RequestContextFactory requestContextFactory, Executor executor) {
            sa.getResourceInterceptors().initializeDefaultFactories(factoryCreator);
            sa.getExceptionMappers().initializeDefaultFactories(factoryCreator);
            sa.getContextResolvers().initializeDefaultFactories(factoryCreator);
            sa.getScannedFeatures().initializeDefaultFactories(factoryCreator);
            sa.getDynamicFeatures().initializeDefaultFactories(factoryCreator);
            sa.getParamConverters().initializeDefaultFactories(factoryCreator);
            sa.getParamConverters().sort();
            sa.getResourceInterceptors().sort();
            sa.getResourceInterceptors().getContainerRequestFilters().validateThreadModel();
            for (var cl : sa.getResourceClasses()) {
                if (cl.getFactory() == null) {
                    cl.setFactory((BeanFactory<Object>) factoryCreator.apply(cl.getClassName()));
                }
            }
            for (var cl : sa.getSubResourceClasses()) {
                if (cl.getFactory() == null) {
                    cl.setFactory((BeanFactory<Object>) factoryCreator.apply(cl.getClassName()));
                }
            }

            DeploymentInfo info = new DeploymentInfo()
                    .setResteasyReactiveConfig(new ResteasyReactiveConfig())
                    .setFeatures(sa.scannedFeatures)
                    .setInterceptors(sa.resourceInterceptors)
                    .setDynamicFeatures(sa.dynamicFeatures)
                    .setParamConverterProviders(sa.paramConverters)
                    .setSerialisers(serialisers)
                    .setExceptionMapping(sa.exceptionMappers)
                    .setResourceClasses(sa.resourceClasses)
                    .setCtxResolvers(sa.contextResolvers)
                    .setLocatableResourceClasses(sa.subResourceClasses)
                    .setFactoryCreator(ReflectiveContextInjectedBeanFactory.FACTORY)
                    .setApplicationSupplier(new Supplier<Application>() {
                        @Override
                        public Application get() {
                            //TODO: make pluggable
                            if (sa.applicationScanningResult.getSelectedAppClass() == null) {
                                return new Application();
                            } else {
                                try {
                                    return (Application) Class
                                            .forName(sa.applicationScanningResult.getSelectedAppClass().name()
                                                    .toString(), false, classLoader)
                                            .getDeclaredConstructor()
                                            .newInstance();
                                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                                        | NoSuchMethodException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
            String path;
            if (scanStep.applicationPath != null) {
                path = scanStep.getApplicationPath();
            } else {
                path = getApplicationPath();
            }
            info.setApplicationPath(path);
            List<Closeable> closeTasks = new ArrayList<>();
            Supplier<Executor> executorSupplier = new Supplier<Executor>() {
                @Override
                public Executor get() {
                    return executor;
                }
            };
            RuntimeDeploymentManager runtimeDeploymentManager = new RuntimeDeploymentManager(info, executorSupplier,
                    executorSupplier,
                    closeTasks::add, requestContextFactory, ThreadSetupAction.NOOP, "/");
            Deployment deployment = runtimeDeploymentManager.deploy();
            deployment.setRuntimeConfiguration(runtimeConfiguration);
            RestInitialHandler initialHandler = new RestInitialHandler(deployment);
            List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers = deployment
                    .getRuntimeConfigurableServerRestHandlers();
            for (RuntimeConfigurableServerRestHandler handler : runtimeConfigurableServerRestHandlers) {
                handler.configure(runtimeConfiguration);
            }
            return new RunnableApplication(closeTasks, initialHandler, deployment, path);
        }

        private String getApplicationPath() {
            String path = "/";
            if (sa.applicationScanningResult.getSelectedAppClass() != null) {
                var pathAn = sa.applicationScanningResult.getSelectedAppClass()
                        .classAnnotation(ResteasyReactiveDotNames.APPLICATION_PATH);
                if (pathAn != null) {
                    path = pathAn.value().asString();
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                }
            }
            return path;
        }
    }

    public static class RunnableApplication implements AutoCloseable {
        final List<Closeable> closeTasks;
        final RestInitialHandler initialHandler;
        final Deployment deployment;
        final String path;

        public RunnableApplication(List<Closeable> closeTasks, RestInitialHandler initialHandler, Deployment deployment,
                String path) {
            this.closeTasks = closeTasks;
            this.initialHandler = initialHandler;
            this.deployment = deployment;
            this.path = path;
        }

        public Deployment getDeployment() {
            return deployment;
        }

        @Override
        public void close() {
            for (var task : closeTasks) {
                try {
                    task.close();
                } catch (IOException e) {
                    log.error("Failed to run close task", e);
                }
            }
        }

        public String getPath() {
            return path;
        }

        public RestInitialHandler getInitialHandler() {
            return initialHandler;
        }
    }

}
