package io.quarkus.test.component;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.All;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.Beans;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.arc.processor.BuiltinBean;
import io.quarkus.arc.processor.BytecodeTransformer;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;
import io.quarkus.arc.processor.ResourceOutput;
import io.quarkus.arc.processor.Types;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTestCallbacks.BeforeBuildContext;
import io.quarkus.test.component.QuarkusComponentTestCallbacks.BeforeIndexContext;
import io.smallrye.config.ConfigMapping;

class ComponentContainer {

    private static final Logger LOG = Logger.getLogger(ComponentContainer.class);

    /**
     * Performs the build for the given test class and configuration.
     *
     * @param testClass
     * @param configuration
     * @param buildShouldFail
     * @param tracedClasses
     * @return the build result
     */
    static BuildResult build(Class<?> testClass, QuarkusComponentTestConfiguration configuration, boolean buildShouldFail,
            Set<String> tracedClasses) {

        if (configuration.componentClasses.isEmpty()) {
            throw new IllegalStateException("No component classes to test");
        }
        long start = System.nanoTime();

        if (LOG.isDebugEnabled()) {
            LOG.debugf("Tested components: \n - %s",
                    configuration.componentClasses.stream().map(Object::toString).collect(Collectors.joining("\n - ")));
        }

        // Build index
        IndexView index;
        try {
            Indexer indexer = new Indexer();
            for (Class<?> componentClass : configuration.componentClasses) {
                // Make sure that component hierarchy and all annotations present are indexed
                indexComponentClass(indexer, componentClass);
            }
            if (configuration.hasCallbacks()) {
                BeforeIndexContextImpl context = new BeforeIndexContextImpl(testClass, configuration.componentClasses);
                for (QuarkusComponentTestCallbacks callback : configuration.callbacks) {
                    callback.beforeIndex(context);
                }
                for (Class<?> clazz : context.additionalComponentsClasses) {
                    indexComponentClass(indexer, clazz);
                }
            }

            indexer.indexClass(ConfigProperty.class);
            index = BeanArchives.buildImmutableBeanArchiveIndex(indexer.complete());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

        ClassLoader testClassLoader = testClass.getClassLoader();
        boolean isContinuousTesting = Conditions.isContinuousTestingDiscovery();

        IndexView computingIndex = BeanArchives.buildComputingBeanArchiveIndex(testClassLoader,
                new ConcurrentHashMap<>(), index);

        Map<String, byte[]> generatedClasses = new HashMap<>();
        AtomicReference<byte[]> componentsProvider = new AtomicReference<>();
        Map<String, Set<String>> configMappings = new HashMap<>();
        Map<String, String[]> interceptorMethods = new HashMap<>();
        Throwable buildFailure = null;
        List<BytecodeTransformer> bytecodeTransformers = new ArrayList<>();
        List<AnnotationTransformation> annotationTransformations = new ArrayList<>();
        for (AnnotationsTransformer transformer : configuration.annotationsTransformers) {
            annotationTransformations.add(transformer);
        }
        List<BeanRegistrar> beanRegistrars = new ArrayList<>();

        if (configuration.hasCallbacks()) {
            BeforeBuildContext beforeBuildContext = new BeforeBulidContextImpl(testClass, index, computingIndex,
                    bytecodeTransformers, annotationTransformations, beanRegistrars);
            for (QuarkusComponentTestCallbacks callback : configuration.callbacks) {
                callback.beforeBuild(beforeBuildContext);
            }
        }

        try {
            // These are populated after BeanProcessor.registerCustomContexts() is called
            List<DotName> qualifiers = new ArrayList<>();
            Set<String> interceptorBindings = new HashSet<>();
            AtomicReference<BeanResolver> beanResolver = new AtomicReference<>();

            // Collect all @Inject and @InjectMock test class injection points to define a bean removal exclusion
            List<Field> injectFields = findInjectFields(testClass, true);
            List<Parameter> injectParams = findInjectParams(testClass);

            String beanProcessorName = testClass.getName().replace('.', '_');
            AtomicReference<BeanDeployment> beanDeployment = new AtomicReference<>();

            BeanProcessor.Builder builder = BeanProcessor.builder()
                    .setName(beanProcessorName)
                    .addRemovalExclusion(b -> {
                        // Do not remove beans:
                        // 1. Annotated with @Unremovable
                        // 2. Injected in the test class or in a test method parameter
                        if (b.getTarget().isPresent()
                                && beanDeployment.get().hasAnnotation(b.getTarget().get(), DotNames.UNREMOVABLE)) {
                            return true;
                        }
                        for (Field injectionPoint : injectFields) {
                            if (injectionPointMatchesBean(injectionPoint.getGenericType(), injectionPoint, qualifiers,
                                    beanResolver.get(), b)) {
                                return true;
                            }
                        }
                        for (Parameter param : injectParams) {
                            if (injectionPointMatchesBean(param.getParameterizedType(), param, qualifiers, beanResolver.get(),
                                    b)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .setImmutableBeanArchiveIndex(index)
                    .setComputingBeanArchiveIndex(computingIndex)
                    .setRemoveUnusedBeans(true)
                    .setTransformUnproxyableClasses(true);

            Path generatedClassesDirectory;

            if (isContinuousTesting) {
                generatedClassesDirectory = null;
            } else {
                File testOutputDirectory = getTestOutputDirectory(testClass);
                generatedClassesDirectory = testOutputDirectory.getParentFile()
                        .toPath()
                        .resolve("generated-classes")
                        .resolve(beanProcessorName);
                Files.createDirectories(generatedClassesDirectory);
            }

            builder.setOutput(new ResourceOutput() {
                @Override
                public void writeResource(Resource resource) throws IOException {
                    switch (resource.getType()) {
                        case JAVA_CLASS:
                            generatedClasses.put(resource.getFullyQualifiedName(), resource.getData());
                            if (generatedClassesDirectory != null) {
                                // debug generated bytecode
                                resource.writeTo(generatedClassesDirectory.toFile());
                            }
                            break;
                        case SERVICE_PROVIDER:
                            if (resource.getName()
                                    .equals(ComponentsProvider.class.getName())) {
                                componentsProvider.set(resource.getData());
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported resource type: " + resource.getType());
                    }
                }
            });

            builder.addAnnotationTransformation(AnnotationsTransformer.appliedToField().whenContainsAny(qualifiers)
                    .whenContainsNone(DotName.createSimple(Inject.class)).thenTransform(t -> t.add(Inject.class)));

            builder.addAnnotationTransformation(new JaxrsSingletonTransformer());
            for (AnnotationTransformation transformation : annotationTransformations) {
                builder.addAnnotationTransformation(transformation);
            }

            // Register:
            // 1) Dummy mock beans for all unsatisfied injection points
            // 2) Synthetic beans for Config and @ConfigProperty injection points
            builder.addBeanRegistrar(new BeanRegistrar() {

                @Override
                public void register(RegistrationContext registrationContext) {
                    long start = System.nanoTime();
                    List<BeanInfo> beans = registrationContext.beans().collect();
                    BeanDeployment beanDeployment = registrationContext.get(Key.DEPLOYMENT);
                    Set<TypeAndQualifiers> unsatisfiedInjectionPoints = new HashSet<>();
                    boolean configInjectionPoint = false;
                    Set<TypeAndQualifiers> configPropertyInjectionPoints = new HashSet<>();
                    DotName configDotName = DotName.createSimple(Config.class);
                    DotName configPropertyDotName = DotName.createSimple(ConfigProperty.class);
                    DotName configMappingDotName = DotName.createSimple(ConfigMapping.class);

                    // We need to analyze all injection points in order to find
                    // Config, @ConfigProperty and config mappings injection points
                    // and all unsatisfied injection points
                    // to register appropriate synthetic beans
                    for (InjectionPointInfo injectionPoint : registrationContext.getInjectionPoints()) {
                        if (injectionPoint.getRequiredType().name().equals(configDotName)
                                && injectionPoint.hasDefaultedQualifier()) {
                            configInjectionPoint = true;
                            continue;
                        }
                        if (injectionPoint.getRequiredQualifier(configPropertyDotName) != null) {
                            configPropertyInjectionPoints.add(new TypeAndQualifiers(injectionPoint.getRequiredType(),
                                    injectionPoint.getRequiredQualifiers()));
                            continue;
                        }
                        BuiltinBean builtin = BuiltinBean.resolve(injectionPoint);
                        if (builtin != null && builtin != BuiltinBean.INSTANCE && builtin != BuiltinBean.LIST) {
                            continue;
                        }
                        Type requiredType = injectionPoint.getRequiredType();
                        Set<AnnotationInstance> requiredQualifiers = injectionPoint.getRequiredQualifiers();
                        if (builtin == BuiltinBean.LIST) {
                            // @All List<Delta> -> Delta
                            requiredType = requiredType.asParameterizedType().arguments().get(0);
                            requiredQualifiers = new HashSet<>(requiredQualifiers);
                            requiredQualifiers.removeIf(q -> q.name().equals(DotNames.ALL));
                            if (requiredQualifiers.isEmpty()) {
                                requiredQualifiers.add(AnnotationInstance.builder(DotNames.DEFAULT).build());
                            }
                        }
                        if (requiredType.kind() == Kind.CLASS) {
                            ClassInfo clazz = computingIndex.getClassByName(requiredType.name());
                            if (clazz != null && clazz.isInterface()) {
                                AnnotationInstance configMapping = clazz.declaredAnnotation(configMappingDotName);
                                if (configMapping != null) {
                                    AnnotationValue prefixValue = configMapping.value("prefix");
                                    String prefix = prefixValue == null ? "" : prefixValue.asString();
                                    Set<String> mappingClasses = configMappings.computeIfAbsent(prefix,
                                            k -> new HashSet<>());
                                    mappingClasses.add(clazz.name().toString());
                                }
                            }
                        }
                        if (isSatisfied(requiredType, requiredQualifiers, injectionPoint, beans, beanDeployment,
                                configuration)) {
                            continue;
                        }
                        if (requiredType.kind() == Kind.PRIMITIVE || requiredType.kind() == Kind.ARRAY) {
                            throw new IllegalStateException(
                                    "Found an unmockable unsatisfied injection point: " + injectionPoint.getTargetInfo());
                        }
                        unsatisfiedInjectionPoints.add(new TypeAndQualifiers(requiredType, requiredQualifiers));
                        LOG.debugf("Unsatisfied injection point found: %s", injectionPoint.getTargetInfo());
                    }

                    // Make sure that all @InjectMock injection points are also considered unsatisfied dependencies
                    // This means that a mock is created even if no component declares this dependency
                    for (Field field : findFields(testClass, List.of(InjectMock.class))) {
                        Set<AnnotationInstance> requiredQualifiers = getQualifiers(field, qualifiers);
                        if (requiredQualifiers.isEmpty()) {
                            requiredQualifiers = Set.of(AnnotationInstance.builder(DotNames.DEFAULT).build());
                        }
                        unsatisfiedInjectionPoints
                                .add(new TypeAndQualifiers(Types.jandexType(field.getGenericType()), requiredQualifiers));
                    }
                    for (Parameter param : findInjectMockParams(testClass)) {
                        Set<AnnotationInstance> requiredQualifiers = getQualifiers(param, qualifiers);
                        if (requiredQualifiers.isEmpty()) {
                            requiredQualifiers = Set.of(AnnotationInstance.builder(DotNames.DEFAULT).build());
                        }
                        unsatisfiedInjectionPoints
                                .add(new TypeAndQualifiers(Types.jandexType(param.getParameterizedType()), requiredQualifiers));
                    }

                    for (TypeAndQualifiers unsatisfied : unsatisfiedInjectionPoints) {
                        ClassInfo implementationClass = computingIndex.getClassByName(unsatisfied.type.name());
                        BeanConfigurator<Object> configurator = registrationContext.configure(implementationClass.name())
                                .scope(Singleton.class)
                                .addType(unsatisfied.type);
                        unsatisfied.qualifiers.forEach(configurator::addQualifier);
                        configurator.param("implementationClass", implementationClass)
                                .creator(MockBeanCreator.class)
                                .defaultBean()
                                .identifier("dummy")
                                .done();
                    }

                    if (configInjectionPoint) {
                        registrationContext.configure(Config.class)
                                .addType(Config.class)
                                .creator(ConfigBeanCreator.class)
                                .done();
                    }

                    if (!configPropertyInjectionPoints.isEmpty()) {
                        BeanConfigurator<Object> configPropertyConfigurator = registrationContext.configure(Object.class)
                                .identifier("configProperty")
                                .addQualifier(ConfigProperty.class)
                                .param("useDefaultConfigProperties", configuration.useDefaultConfigProperties)
                                .addInjectionPoint(ClassType.create(InjectionPoint.class))
                                .creator(ConfigPropertyBeanCreator.class);
                        for (TypeAndQualifiers configPropertyInjectionPoint : configPropertyInjectionPoints) {
                            configPropertyConfigurator.addType(configPropertyInjectionPoint.type);
                        }
                        configPropertyConfigurator.done();
                    }

                    if (!configMappings.isEmpty()) {
                        for (Entry<String, Set<String>> e : configMappings.entrySet()) {
                            for (String mapping : e.getValue()) {
                                DotName mappingName = DotName.createSimple(mapping);
                                registrationContext.configure(mappingName)
                                        .addType(mappingName)
                                        .creator(ConfigMappingBeanCreator.class)
                                        .param("mappingClass", mapping)
                                        .param("prefix", e.getKey())
                                        .done();
                            }
                        }
                    }

                    LOG.debugf("Test injection points analyzed in %s ms [found: %s, mocked: %s]",
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                            registrationContext.getInjectionPoints().size(),
                            unsatisfiedInjectionPoints.size());

                    // Find all methods annotated with interceptor annotations and register them as synthetic interceptors
                    processTestInterceptorMethods(testClass, registrationContext, interceptorBindings, interceptorMethods);
                }
            });

            // Register mock beans
            for (MockBeanConfiguratorImpl<?> mockConfigurator : configuration.mockConfigurators) {
                builder.addBeanRegistrar(registrarForMock(testClass, mockConfigurator));
            }
            // Synthetic beans from callbacks
            for (BeanRegistrar beanRegistrar : beanRegistrars) {
                builder.addBeanRegistrar(beanRegistrar);
            }

            // Process the deployment
            BeanProcessor beanProcessor = builder.build();
            beanDeployment.set(beanProcessor.getBeanDeployment());
            try {
                Consumer<BytecodeTransformer> bytecodeTransformerConsumer = bytecodeTransformers::add;
                // Populate the list of qualifiers used to simulate quarkus auto injection
                ContextRegistrar.RegistrationContext registrationContext = beanProcessor.registerCustomContexts();
                qualifiers.addAll(registrationContext.get(Key.QUALIFIERS).keySet());
                for (DotName binding : registrationContext.get(Key.INTERCEPTOR_BINDINGS).keySet()) {
                    interceptorBindings.add(binding.toString());
                }
                beanResolver.set(registrationContext.get(Key.DEPLOYMENT).getBeanResolver());
                beanProcessor.registerScopes();
                beanProcessor.registerBeans();
                beanProcessor.getBeanDeployment().initBeanByTypeMap();
                beanProcessor.registerSyntheticObservers();
                beanProcessor.initialize(bytecodeTransformerConsumer, Collections.emptyList());
                ValidationContext validationContext = beanProcessor.validate(bytecodeTransformerConsumer);
                beanProcessor.processValidationErrors(validationContext);
                // Generate resources in parallel
                ExecutorService executor = Executors.newCachedThreadPool();
                beanProcessor.generateResources(null, new HashSet<>(), bytecodeTransformerConsumer, true, executor);
                executor.shutdown();

                Map<String, byte[]> transformedClasses = new HashMap<>();
                Path transformedClassesDirectory = null;
                if (!isContinuousTesting) {
                    File testOutputDirectory = getTestOutputDirectory(testClass);
                    transformedClassesDirectory = testOutputDirectory.getParentFile().toPath()
                            .resolve("transformed-classes").resolve(beanProcessorName);
                    Files.createDirectories(transformedClassesDirectory);
                }

                // Make sure the traced classes are transformed in continuous testing
                for (String tracedClass : tracedClasses) {
                    if (tracedClass.startsWith("io.quarkus.test.component")) {
                        continue;
                    }
                    bytecodeTransformers.add(new BytecodeTransformer(tracedClass, (cn, cv) -> new TracingClassVisitor(cv, cn)));
                }

                if (!bytecodeTransformers.isEmpty()) {
                    Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> map = bytecodeTransformers.stream()
                            .collect(Collectors.groupingBy(BytecodeTransformer::getClassToTransform,
                                    Collectors.mapping(BytecodeTransformer::getVisitorFunction, Collectors.toList())));

                    for (Map.Entry<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> entry : map.entrySet()) {
                        String className = entry.getKey();
                        List<BiFunction<String, ClassVisitor, ClassVisitor>> transformations = entry.getValue();

                        String classFileName = className.replace('.', '/') + ".class";
                        byte[] bytecode;
                        try (InputStream in = testClassLoader.getResourceAsStream(classFileName)) {
                            if (in == null) {
                                throw new IOException("Resource not found: " + classFileName);
                            }
                            bytecode = in.readAllBytes();
                        }
                        ClassReader reader = new ClassReader(bytecode);
                        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        ClassVisitor visitor = writer;
                        for (BiFunction<String, ClassVisitor, ClassVisitor> transformation : transformations) {
                            visitor = transformation.apply(className, visitor);
                        }
                        reader.accept(visitor, 0);
                        bytecode = writer.toByteArray();
                        transformedClasses.put(className, bytecode);

                        if (transformedClassesDirectory != null) {
                            // debug generated bytecode
                            Path classFile = transformedClassesDirectory.resolve(
                                    classFileName.replace('/', '_').replace('$', '_'));
                            Files.write(classFile, bytecode);
                        }
                    }
                }
                generatedClasses.putAll(transformedClasses);

            } catch (IOException e) {
                throw new IllegalStateException("Error generating resources", e);
            }

        } catch (Throwable e) {
            if (buildShouldFail) {
                buildFailure = e;
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (buildShouldFail && buildFailure == null) {
                throw new AssertionError("The container build was expected to fail!");
            }
        }

        LOG.debugf("Component container for %s built in %s ms, using CL: %s", testClass.getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                ComponentContainer.class.getClassLoader().getClass().getSimpleName());
        return new BuildResult(generatedClasses, componentsProvider.get(), configMappings, interceptorMethods,
                buildFailure);
    }

    private static BeanRegistrar registrarForMock(Class<?> testClass, MockBeanConfiguratorImpl<?> mock) {
        return new BeanRegistrar() {

            @Override
            public void register(RegistrationContext context) {
                BeanConfigurator<Object> configurator = context.configure(mock.beanClass);
                configurator.scope(mock.scope);
                mock.jandexTypes().forEach(configurator::addType);
                mock.jandexQualifiers().forEach(configurator::addQualifier);
                if (mock.name != null) {
                    configurator.name(mock.name);
                }
                configurator.alternative(mock.alternative);
                if (mock.priority != null) {
                    configurator.priority(mock.priority);
                }
                if (mock.defaultBean) {
                    configurator.defaultBean();
                }
                String key = MockBeanCreator.registerCreate(testClass.getName(), cast(mock.create));
                configurator.creator(MockBeanCreator.class).param(MockBeanCreator.CREATE_KEY, key).done();
            }
        };
    }

    private static void indexComponentClass(Indexer indexer, Class<?> componentClass) {
        try {
            while (componentClass != null) {
                indexer.indexClass(componentClass);
                for (Annotation annotation : componentClass.getAnnotations()) {
                    indexer.indexClass(annotation.annotationType());
                }
                for (Field field : componentClass.getDeclaredFields()) {
                    indexAnnotatedElement(indexer, field);
                }
                for (Method method : componentClass.getDeclaredMethods()) {
                    indexAnnotatedElement(indexer, method);
                    for (Parameter param : method.getParameters()) {
                        indexAnnotatedElement(indexer, param);
                    }
                }
                for (Class<?> iface : componentClass.getInterfaces()) {
                    indexComponentClass(indexer, iface);
                }
                componentClass = componentClass.getSuperclass();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to index:" + componentClass, e);
        }
    }

    private static void indexAnnotatedElement(Indexer indexer, AnnotatedElement element) throws IOException {
        for (Annotation annotation : element.getAnnotations()) {
            indexer.indexClass(annotation.annotationType());
        }
    }

    private static List<Field> findInjectFields(Class<?> testClass, boolean scanEnclosingClasses) {
        List<Class<? extends Annotation>> injectAnnotations;

        Class<? extends Annotation> injectSpy = loadInjectSpy();
        if (injectSpy != null) {
            injectAnnotations = List.of(Inject.class, InjectMock.class, injectSpy);
        } else {
            injectAnnotations = List.of(Inject.class, InjectMock.class);
        }

        List<Field> found = findFields(testClass, injectAnnotations);
        if (scanEnclosingClasses) {
            Class<?> enclosing = testClass.getEnclosingClass();
            while (enclosing != null) {
                // @Nested test class
                found.addAll(findFields(enclosing, injectAnnotations));
                enclosing = enclosing.getEnclosingClass();
            }
        }

        if (injectSpy != null) {
            List<Field> injectSpies = found.stream().filter(f -> f.isAnnotationPresent(injectSpy)).toList();
            if (!injectSpies.isEmpty()) {
                throw new IllegalStateException("@InjectSpy is not supported by QuarkusComponentTest: " + injectSpies);
            }
        }

        return found;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadInjectSpy() {
        try {
            return (Class<? extends Annotation>) Class.forName("io.quarkus.test.junit.mockito.InjectSpy");
        } catch (Throwable e) {
            return null;
        }
    }

    static final Predicate<Parameter> BUILTIN_PARAMETER = new Predicate<Parameter>() {

        @Override
        public boolean test(Parameter parameter) {
            if (parameter.isAnnotationPresent(TempDir.class)) {
                return true;
            }
            java.lang.reflect.Type type = parameter.getParameterizedType();
            return type.equals(TestInfo.class) || type.equals(RepetitionInfo.class) || type.equals(TestReporter.class);
        }
    };

    private static List<Parameter> findInjectParams(Class<?> testClass) {
        List<Method> testMethods = findMethods(testClass, QuarkusComponentTestExtension::isTestMethod);
        List<Parameter> ret = new ArrayList<>();
        for (Method method : testMethods) {
            for (Parameter param : method.getParameters()) {
                if (BUILTIN_PARAMETER.test(param)
                        || param.isAnnotationPresent(SkipInject.class)) {
                    continue;
                }
                ret.add(param);
            }
        }
        return ret;
    }

    private static List<Parameter> findInjectMockParams(Class<?> testClass) {
        List<Method> testMethods = findMethods(testClass, QuarkusComponentTestExtension::isTestMethod);
        List<Parameter> ret = new ArrayList<>();
        for (Method method : testMethods) {
            for (Parameter param : method.getParameters()) {
                if (param.isAnnotationPresent(InjectMock.class)
                        && !BUILTIN_PARAMETER.test(param)) {
                    ret.add(param);
                }
            }
        }
        return ret;
    }

    static boolean isTestMethod(Executable method) {
        return method.isAnnotationPresent(Test.class)
                || method.isAnnotationPresent(ParameterizedTest.class)
                || method.isAnnotationPresent(RepeatedTest.class);
    }

    private static List<Field> findFields(Class<?> testClass, List<Class<? extends Annotation>> annotations) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                for (Class<? extends Annotation> annotation : annotations) {
                    if (field.isAnnotationPresent(annotation)) {
                        fields.add(field);
                        break;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static List<Method> findMethods(Class<?> testClass, Predicate<Method> methodPredicate) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (methodPredicate.test(method)) {
                    methods.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    private static Set<AnnotationInstance> getQualifiers(AnnotatedElement element, Collection<DotName> qualifiers) {
        Set<AnnotationInstance> ret = new HashSet<>();
        Annotation[] annotations = element.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (qualifiers.contains(DotName.createSimple(annotation.annotationType()))) {
                ret.add(Annotations.jandexAnnotation(annotation));
            }
        }
        return ret;
    }

    private static boolean isListRequiredType(java.lang.reflect.Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return List.class.equals(parameterizedType.getRawType());
        }
        return false;
    }

    static boolean isListAllInjectionPoint(java.lang.reflect.Type requiredType, Annotation[] qualifiers,
            AnnotatedElement annotatedElement) {
        if (qualifiers.length > 0 && Arrays.stream(qualifiers).anyMatch(All.Literal.INSTANCE::equals)) {
            if (!isListRequiredType(requiredType)) {
                throw new IllegalStateException("Invalid injection point type: " + annotatedElement);
            }
            return true;
        }
        return false;
    }

    static final DotName ALL_NAME = DotName.createSimple(All.class);

    static void adaptListAllQualifiers(Set<AnnotationInstance> qualifiers) {
        // Remove @All and add @Default if empty
        qualifiers.removeIf(a -> a.name().equals(ALL_NAME));
        if (qualifiers.isEmpty()) {
            qualifiers.add(AnnotationInstance.builder(Default.class).build());
        }
    }

    static java.lang.reflect.Type getFirstActualTypeArgument(java.lang.reflect.Type requiredType) {
        if (requiredType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) requiredType;
            // List<String> -> String
            return parameterizedType.getActualTypeArguments()[0];
        }
        return null;
    }

    private static boolean injectionPointMatchesBean(java.lang.reflect.Type injectionPointType,
            AnnotatedElement annotatedElement,
            List<DotName> allQualifiers, BeanResolver beanResolver, BeanInfo bean) {
        Type requiredType;
        Set<AnnotationInstance> requiredQualifiers = getQualifiers(annotatedElement, allQualifiers);
        if (isListAllInjectionPoint(injectionPointType,
                Arrays.stream(annotatedElement.getAnnotations())
                        .filter(a -> allQualifiers.contains(DotName.createSimple(a.annotationType())))
                        .toArray(Annotation[]::new),
                annotatedElement)) {
            requiredType = Types.jandexType(getFirstActualTypeArgument(injectionPointType));
            adaptListAllQualifiers(requiredQualifiers);
        } else if (Instance.class.isAssignableFrom(QuarkusComponentTestConfiguration.getRawType(injectionPointType))) {
            requiredType = Types.jandexType(getFirstActualTypeArgument(injectionPointType));
        } else {
            requiredType = Types.jandexType(injectionPointType);
        }
        return beanResolver.matches(bean, requiredType, requiredQualifiers);
    }

    private static final String QUARKUS_TEST_COMPONENT_OUTPUT_DIRECTORY = "quarkus.test.component.output-directory";

    private static File getTestOutputDirectory(Class<?> testClass) {
        String outputDirectory = System.getProperty(QUARKUS_TEST_COMPONENT_OUTPUT_DIRECTORY);
        File testOutputDirectory;
        if (outputDirectory != null) {
            testOutputDirectory = new File(outputDirectory);
        } else {
            // All below string transformations work with _URL encoded_ paths, where e.g.
            // a space is replaced with %20. At the end, we feed this back to URI.create
            // to make sure the encoding is dealt with properly, so we don't have to do this
            // ourselves. Directly passing a URL-encoded string to the File() constructor
            // does not work properly.

            // org.acme.Foo -> org/acme/Foo.class
            String testClassResourceName = fromClassNameToResourceName(testClass.getName());
            // org/acme/Foo.class -> file:/some/path/to/project/target/test-classes/org/acme/Foo.class
            String testPath = testClass.getClassLoader().getResource(testClassResourceName).toString();
            // file:/some/path/to/project/target/test-classes/org/acme/Foo.class -> file:/some/path/to/project/target/test-classes
            String testClassesRootPath = testPath.substring(0, testPath.length() - testClassResourceName.length() - 1);
            // resolve back to File instance
            testOutputDirectory = new File(URI.create(testClassesRootPath));
        }
        if (!testOutputDirectory.canWrite()) {
            throw new IllegalStateException("Invalid test output directory: " + testOutputDirectory);
        }
        return testOutputDirectory;
    }

    private static boolean isSatisfied(Type requiredType, Set<AnnotationInstance> qualifiers, InjectionPointInfo injectionPoint,
            Iterable<BeanInfo> beans, BeanDeployment beanDeployment, QuarkusComponentTestConfiguration configuration) {
        for (BeanInfo bean : beans) {
            if (Beans.matches(bean, requiredType, qualifiers)) {
                LOG.debugf("Injection point %s satisfied by %s", injectionPoint.getTargetInfo(),
                        bean.toString());
                return true;
            }
        }
        for (MockBeanConfiguratorImpl<?> mock : configuration.mockConfigurators) {
            if (mock.matches(beanDeployment.getBeanResolver(), requiredType, qualifiers)) {
                LOG.debugf("Injection point %s satisfied by %s", injectionPoint.getTargetInfo(),
                        mock);
                return true;
            }
        }
        return false;
    }

    private static void processTestInterceptorMethods(Class<?> testClass,
            BeanRegistrar.RegistrationContext registrationContext, Set<String> interceptorBindings,
            Map<String, String[]> interceptorMethods) {
        List<Class<? extends Annotation>> annotations = List.of(AroundInvoke.class, PostConstruct.class, PreDestroy.class,
                AroundConstruct.class);
        Predicate<Method> predicate = m -> {
            for (Class<? extends Annotation> annotation : annotations) {
                if (m.isAnnotationPresent(annotation)) {
                    return true;
                }
            }
            return false;
        };
        for (Method method : findMethods(testClass, predicate)) {
            Set<Annotation> bindings = findBindings(method, interceptorBindings);
            if (bindings.isEmpty()) {
                throw new IllegalStateException("No bindings declared on a test interceptor method: " + method);
            }
            validateTestInterceptorMethod(method);
            String key = UUID.randomUUID().toString();
            interceptorMethods.put(key, InterceptorMethodCreator.descriptor(method));
            InterceptionType interceptionType;
            if (method.isAnnotationPresent(AroundInvoke.class)) {
                interceptionType = InterceptionType.AROUND_INVOKE;
            } else if (method.isAnnotationPresent(PostConstruct.class)) {
                interceptionType = InterceptionType.POST_CONSTRUCT;
            } else if (method.isAnnotationPresent(PreDestroy.class)) {
                interceptionType = InterceptionType.PRE_DESTROY;
            } else if (method.isAnnotationPresent(AroundConstruct.class)) {
                interceptionType = InterceptionType.AROUND_CONSTRUCT;
            } else {
                // This should never happen
                throw new IllegalStateException("No interceptor annotation declared on: " + method);
            }
            int priority = 1;
            Priority priorityAnnotation = method.getAnnotation(Priority.class);
            if (priorityAnnotation != null) {
                priority = priorityAnnotation.value();
            }
            registrationContext.configureInterceptor(interceptionType)
                    .identifier(key)
                    .priority(priority)
                    .bindings(bindings.stream().map(Annotations::jandexAnnotation)
                            .toArray(AnnotationInstance[]::new))
                    .param(InterceptorMethodCreator.CREATE_KEY, key)
                    .creator(InterceptorMethodCreator.class);
        }
    }

    private static void validateTestInterceptorMethod(Method method) {
        Parameter[] params = method.getParameters();
        if (params.length != 1 || !InvocationContext.class.isAssignableFrom(params[0].getType())) {
            throw new IllegalStateException("A test interceptor method must declare exactly one InvocationContext parameter:"
                    + Arrays.toString(params));
        }

    }

    private static Set<Annotation> findBindings(Method method, Set<String> bindings) {
        return Arrays.stream(method.getAnnotations()).filter(a -> bindings.contains(a.annotationType().getName()))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static class TracingClassVisitor extends ClassVisitor {

        private final String className;

        public TracingClassVisitor(ClassVisitor classVisitor, String theClassName) {
            super(Gizmo.ASM_API_VERSION, classVisitor);
            this.className = theClassName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<init>") || name.equals("<clinit>")) {
                return mv;
            }
            LOG.debugf("Trace method %s#%s:%s", className, name, descriptor);
            return new MethodVisitor(Gizmo.ASM_API_VERSION, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();
                    visitLdcInsn(className);
                    visitMethodInsn(Opcodes.INVOKESTATIC,
                            TracingHandler.class.getName().replace(".", "/"), "trace",
                            "(Ljava/lang/String;)V", false);
                }
            };
        }
    }

    private static class BeforeIndexContextImpl extends QuarkusComponentTestExtension.ComponentTestContextImpl
            implements BeforeIndexContext {

        private final Set<Class<?>> componentClasses;
        private final List<Class<?>> additionalComponentsClasses;

        BeforeIndexContextImpl(Class<?> testClass, Set<Class<?>> componentClasses) {
            super(testClass);
            this.componentClasses = componentClasses;
            this.additionalComponentsClasses = new ArrayList<>();
        }

        @Override
        public Set<Class<?>> getComponentClasses() {
            return componentClasses;
        }

        @Override
        public void addComponentClass(Class<?> componentClass) {
            additionalComponentsClasses.add(componentClass);
        }

    }

    private static class BeforeBulidContextImpl extends QuarkusComponentTestExtension.ComponentTestContextImpl
            implements BeforeBuildContext {

        private final IndexView immutableBeanArchiveIndex;
        private final IndexView computingBeanArchiveIndex;
        private final List<BytecodeTransformer> bytecodeTransformers;
        private final List<AnnotationTransformation> annotationTransformations;
        private final List<BeanRegistrar> beanRegistrars;

        private BeforeBulidContextImpl(Class<?> testClass, IndexView immutableBeanArchiveIndex,
                IndexView computingBeanArchiveIndex, List<BytecodeTransformer> bytecodeTransformers,
                List<AnnotationTransformation> annotationTransformations, List<BeanRegistrar> beanRegistrars) {
            super(testClass);
            this.immutableBeanArchiveIndex = immutableBeanArchiveIndex;
            this.computingBeanArchiveIndex = computingBeanArchiveIndex;
            this.bytecodeTransformers = bytecodeTransformers;
            this.annotationTransformations = annotationTransformations;
            this.beanRegistrars = beanRegistrars;
        }

        @Override
        public IndexView getImmutableBeanArchiveIndex() {
            return immutableBeanArchiveIndex;
        }

        @Override
        public IndexView getComputingBeanArchiveIndex() {
            return computingBeanArchiveIndex;
        }

        @Override
        public void addAnnotationTransformation(AnnotationTransformation transformation) {
            annotationTransformations.add(transformation);
        }

        @Override
        public void addBeanRegistrar(BeanRegistrar beanRegistrar) {
            beanRegistrars.add(beanRegistrar);
        }

        @Override
        public void addBytecodeTransformer(BytecodeTransformer bytecodeTransformer) {
            bytecodeTransformers.add(bytecodeTransformer);
        }

    }

}
