package io.quarkus.test.component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Unremovable;
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
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.runtime.configuration.ApplicationPropertiesConfigSourceLoader;
import io.quarkus.test.InjectMock;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * Makes it easy to test Quarkus components. This extension can be registered declaratively with {@link QuarkusComponentTest} or
 * programmatically with a static field of type {@link QuarkusComponentTestExtension}, annotated with {@link RegisterExtension}
 * and initialized with {@link #QuarkusComponentTestExtension(Class...) simplified constructor} or using the {@link #builder()
 * builder}.
 *
 * <h2>Container lifecycle</h2>
 * <p>
 * This extension starts the CDI container and registers a dedicated SmallRyeConfig. If {@link Lifecycle#PER_METHOD} is used
 * (default) then the container is started during the {@code before each} test phase and stopped during the {@code after each}
 * test phase. However, if {@link Lifecycle#PER_CLASS} is used then the container is started during the {@code before all} test
 * phase and stopped during the {@code after all} test phase. The CDI request context is activated and terminated per each test
 * method.
 *
 * <h2>Injection</h2>
 * <p>
 * Test class fields annotated with {@link jakarta.inject.Inject} and {@link io.quarkus.test.InjectMock} are injected after a
 * test instance is created and unset before a test instance is destroyed. Dependent beans injected into these
 * fields are correctly destroyed before a test instance is destroyed.
 * <p>
 * Parameters of a test method for which a matching bean exists are resolved unless annotated with {@link SkipInject}. Dependent
 * beans injected into the test method arguments are correctly destroyed after the test method completes.
 *
 * <h2>Auto Mocking Unsatisfied Dependencies</h2>
 * <p>
 * Unlike in regular CDI environments the test does not fail if a component injects an unsatisfied dependency. Instead, a
 * synthetic bean is registered automatically for each combination of required type and qualifiers of an injection point that
 * resolves to an unsatisfied dependency. The bean has the {@link Singleton} scope so it's shared across all injection points
 * with the same required type and qualifiers. The injected reference is an unconfigured Mockito mock. You can inject the mock
 * in your test using the {@link io.quarkus.test.InjectMock} annotation and leverage the Mockito API to configure the behavior.
 *
 * <h2>Custom Mocks For Unsatisfied Dependencies</h2>
 * <p>
 * Sometimes you need the full control over the bean attributes and maybe even configure the default mock behavior. You can use
 * the mock configurator API via the {@link QuarkusComponentTestExtensionBuilder#mock(Class)} method.
 *
 * @see InjectMock
 * @see TestConfigProperty
 */
@Experimental("This feature is experimental and the API may change in the future")
public class QuarkusComponentTestExtension
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, TestInstancePostProcessor,
        ParameterResolver {

    public static QuarkusComponentTestExtensionBuilder builder() {
        return new QuarkusComponentTestExtensionBuilder();
    }

    private static final Logger LOG = Logger.getLogger(QuarkusComponentTestExtension.class);

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(QuarkusComponentTestExtension.class);

    // Strings used as keys in ExtensionContext.Store
    private static final String KEY_OLD_TCCL = "oldTccl";
    private static final String KEY_OLD_CONFIG_PROVIDER_RESOLVER = "oldConfigProviderResolver";
    private static final String KEY_GENERATED_RESOURCES = "generatedResources";
    private static final String KEY_INJECTED_FIELDS = "injectedFields";
    private static final String KEY_INJECTED_PARAMS = "injectedParams";
    private static final String KEY_TEST_INSTANCE = "testInstance";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_TEST_CLASS_CONFIG = "testClassConfig";
    private static final String KEY_CONFIG_MAPPINGS = "configMappings";

    private static final String QUARKUS_TEST_COMPONENT_OUTPUT_DIRECTORY = "quarkus.test.component.output-directory";

    private final QuarkusComponentTestConfiguration baseConfiguration;

    // Used for declarative registration
    public QuarkusComponentTestExtension() {
        this(QuarkusComponentTestConfiguration.DEFAULT);
    }

    /**
     * The initial set of components under test is derived from the test class. The types of all fields annotated with
     * {@link jakarta.inject.Inject} are considered the component types.
     *
     * @param additionalComponentClasses
     */
    public QuarkusComponentTestExtension(Class<?>... additionalComponentClasses) {
        this(new QuarkusComponentTestConfiguration(Map.of(), List.of(additionalComponentClasses),
                List.of(), false, true, QuarkusComponentTestExtensionBuilder.DEFAULT_CONFIG_SOURCE_ORDINAL,
                List.of()));
    }

    QuarkusComponentTestExtension(QuarkusComponentTestConfiguration baseConfiguration) {
        this.baseConfiguration = baseConfiguration;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        buildContainer(context);
        startContainer(context, Lifecycle.PER_CLASS);
        LOG.debugf("beforeAll: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        // Stop the container if Lifecycle.PER_CLASS is used
        stopContainer(context, Lifecycle.PER_CLASS);
        cleanup(context);
        LOG.debugf("afterAll: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        startContainer(context, Lifecycle.PER_METHOD);
        // Activate the request context
        Arc.container().requestContext().activate();
        LOG.debugf("beforeEach: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        // Terminate the request context
        Arc.container().requestContext().terminate();
        // Destroy @Dependent beans injected as test method parameters correctly
        destroyDependentTestMethodParams(context);
        // Stop the container if Lifecycle.PER_METHOD is used
        stopContainer(context, Lifecycle.PER_METHOD);
        LOG.debugf("afterEach: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        context.getRoot().getStore(NAMESPACE).put(KEY_TEST_INSTANCE, testInstance);
        LOG.debugf("postProcessTestInstance: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
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

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (
        // Target is empty for constructor or static method
        parameterContext.getTarget().isPresent()
                // Only test methods are supported
                && parameterContext.getDeclaringExecutable().isAnnotationPresent(Test.class)
                // A method/param annotated with @SkipInject is never supported
                && !parameterContext.isAnnotated(SkipInject.class)
                && !parameterContext.getDeclaringExecutable().isAnnotationPresent(SkipInject.class)
                // Skip params covered by built-in extensions
                && !BUILTIN_PARAMETER.test(parameterContext.getParameter())) {
            BeanManager beanManager = Arc.container().beanManager();
            java.lang.reflect.Type requiredType = parameterContext.getParameter().getParameterizedType();
            Annotation[] qualifiers = getQualifiers(parameterContext.getAnnotatedElement(), beanManager);
            if (qualifiers.length > 0 && Arrays.stream(qualifiers).anyMatch(All.Literal.INSTANCE::equals)) {
                // @All List<>
                if (isListRequiredType(requiredType)) {
                    return true;
                } else {
                    throw new IllegalStateException("Invalid injection point type: " + parameterContext.getParameter());
                }
            } else {
                try {
                    Bean<?> bean = beanManager.resolve(beanManager.getBeans(requiredType, qualifiers));
                    if (bean == null) {
                        String msg = String.format("No matching bean found for the type [%s] and qualifiers %s",
                                requiredType, Arrays.toString(qualifiers));
                        if (parameterContext.isAnnotated(InjectMock.class) || qualifiers.length > 0) {
                            throw new IllegalStateException(msg);
                        } else {
                            LOG.info(msg + " - consider annotating the parameter with @SkipInject");
                            return false;
                        }
                    }
                    return true;
                } catch (AmbiguousResolutionException e) {
                    String msg = String.format(
                            "Multiple matching beans found for the type [%s] and qualifiers %s\n\t- if this parameter should not be resolved by CDI then use @SkipInject\n\t- found beans: %s",
                            requiredType, Arrays.toString(qualifiers), e.getMessage());
                    throw new IllegalStateException(msg);
                }
            }
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        @SuppressWarnings("unchecked")
        List<InstanceHandle<?>> injectedParams = context.getRoot().getStore(NAMESPACE).get(KEY_INJECTED_PARAMS, List.class);
        ArcContainer container = Arc.container();
        BeanManager beanManager = container.beanManager();
        java.lang.reflect.Type requiredType = parameterContext.getParameter().getParameterizedType();
        Annotation[] qualifiers = getQualifiers(parameterContext.getAnnotatedElement(), beanManager);
        if (qualifiers.length > 0 && Arrays.stream(qualifiers).anyMatch(All.Literal.INSTANCE::equals)) {
            // Special handling for @Injec @All List<>
            return handleListAll(requiredType, qualifiers, container, injectedParams);
        } else {
            InstanceHandle<?> handle = container.instance(requiredType, qualifiers);
            injectedParams.add(handle);
            return handle.get();
        }
    }

    private void destroyDependentTestMethodParams(ExtensionContext context) {
        @SuppressWarnings("unchecked")
        List<InstanceHandle<?>> injectedParams = context.getRoot().getStore(NAMESPACE).get(KEY_INJECTED_PARAMS, List.class);
        for (InstanceHandle<?> handle : injectedParams) {
            if (handle.getBean() != null && handle.getBean().getScope().equals(Dependent.class)) {
                try {
                    handle.destroy();
                } catch (Exception e) {
                    LOG.errorf(e, "Unable to destroy the injected %s", handle.getBean());
                }
            }
        }
        injectedParams.clear();
    }

    private void buildContainer(ExtensionContext context) {
        QuarkusComponentTestConfiguration testClassConfiguration = baseConfiguration
                .update(context.getRequiredTestClass());
        context.getRoot().getStore(NAMESPACE).put(KEY_TEST_CLASS_CONFIG, testClassConfiguration);
        ClassLoader oldTccl = initArcContainer(context, testClassConfiguration);
        context.getRoot().getStore(NAMESPACE).put(KEY_OLD_TCCL, oldTccl);
    }

    @SuppressWarnings("unchecked")
    private void cleanup(ExtensionContext context) {
        ClassLoader oldTccl = context.getRoot().getStore(NAMESPACE).get(KEY_OLD_TCCL, ClassLoader.class);
        Thread.currentThread().setContextClassLoader(oldTccl);
        context.getRoot().getStore(NAMESPACE).remove(KEY_CONFIG_MAPPINGS);
        Set<Path> generatedResources = context.getRoot().getStore(NAMESPACE).get(KEY_GENERATED_RESOURCES, Set.class);
        for (Path path : generatedResources) {
            try {
                LOG.debugf("Delete generated %s", path);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOG.errorf("Unable to delete the generated resource %s: ", path, e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void stopContainer(ExtensionContext context, Lifecycle testInstanceLifecycle) throws Exception {
        if (testInstanceLifecycle.equals(context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD))) {
            for (FieldInjector fieldInjector : (List<FieldInjector>) context.getRoot().getStore(NAMESPACE)
                    .get(KEY_INJECTED_FIELDS, List.class)) {
                fieldInjector.unset(context.getRequiredTestInstance());
            }
            try {
                Arc.shutdown();
            } catch (Exception e) {
                LOG.error("An error occured during ArC shutdown: " + e);
            }
            MockBeanCreator.clear();
            ConfigBeanCreator.clear();
            InterceptorMethodCreator.clear();

            SmallRyeConfig config = context.getRoot().getStore(NAMESPACE).get(KEY_CONFIG, SmallRyeConfig.class);
            ConfigProviderResolver.instance().releaseConfig(config);
            ConfigProviderResolver
                    .setInstance(context.getRoot().getStore(NAMESPACE).get(KEY_OLD_CONFIG_PROVIDER_RESOLVER,
                            ConfigProviderResolver.class));
        }
    }

    private void startContainer(ExtensionContext context, Lifecycle testInstanceLifecycle) throws Exception {
        if (testInstanceLifecycle.equals(context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD))) {
            // Init ArC
            Arc.initialize();

            QuarkusComponentTestConfiguration configuration = context.getRoot().getStore(NAMESPACE)
                    .get(KEY_TEST_CLASS_CONFIG, QuarkusComponentTestConfiguration.class);
            Optional<Method> testMethod = context.getTestMethod();
            if (testMethod.isPresent()) {
                configuration = configuration.update(testMethod.get());
            }

            ConfigProviderResolver oldConfigProviderResolver = ConfigProviderResolver.instance();
            context.getRoot().getStore(NAMESPACE).put(KEY_OLD_CONFIG_PROVIDER_RESOLVER, oldConfigProviderResolver);

            SmallRyeConfigProviderResolver smallRyeConfigProviderResolver = new SmallRyeConfigProviderResolver();
            ConfigProviderResolver.setInstance(smallRyeConfigProviderResolver);

            // TCCL is now the QuarkusComponentTestClassLoader set during initialization
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder().forClassLoader(tccl)
                    .addDefaultInterceptors()
                    .addDefaultSources()
                    .withSources(new ApplicationPropertiesConfigSourceLoader.InFileSystem())
                    .withSources(new ApplicationPropertiesConfigSourceLoader.InClassPath())
                    .withSources(
                            new QuarkusComponentTestConfigSource(configuration.configProperties,
                                    configuration.configSourceOrdinal));
            @SuppressWarnings("unchecked")
            Set<ConfigClassWithPrefix> configMappings = context.getRoot().getStore(NAMESPACE).get(KEY_CONFIG_MAPPINGS,
                    Set.class);
            if (configMappings != null) {
                // Register the mappings found during bean discovery
                for (ConfigClassWithPrefix mapping : configMappings) {
                    configBuilder.withMapping(mapping.getKlass(), mapping.getPrefix());
                }
            }
            SmallRyeConfig config = configBuilder.build();
            smallRyeConfigProviderResolver.registerConfig(config, tccl);
            context.getRoot().getStore(NAMESPACE).put(KEY_CONFIG, config);
            ConfigBeanCreator.setClassLoader(tccl);

            // Inject fields declated on the test class
            Object testInstance = context.getRequiredTestInstance();
            context.getRoot().getStore(NAMESPACE).put(KEY_INJECTED_FIELDS,
                    injectFields(context.getRequiredTestClass(), testInstance));
            // Injected test method parameters
            context.getRoot().getStore(NAMESPACE).put(KEY_INJECTED_PARAMS, new CopyOnWriteArrayList<>());
        }
    }

    private BeanRegistrar registrarForMock(MockBeanConfiguratorImpl<?> mock) {
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
                String key = UUID.randomUUID().toString();
                MockBeanCreator.registerCreate(key, cast(mock.create));
                configurator.creator(MockBeanCreator.class).param(MockBeanCreator.CREATE_KEY, key).done();
            }
        };
    }

    private static Annotation[] getQualifiers(AnnotatedElement element, BeanManager beanManager) {
        List<Annotation> ret = new ArrayList<>();
        Annotation[] annotations = element.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : annotations) {
            if (beanManager.isQualifier(fieldAnnotation.annotationType())) {
                ret.add(fieldAnnotation);
            }
        }
        return ret.toArray(new Annotation[0]);
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

    private ClassLoader initArcContainer(ExtensionContext extensionContext, QuarkusComponentTestConfiguration configuration) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        // Collect all component injection points to define a bean removal exclusion
        List<Field> injectFields = findInjectFields(testClass);
        List<Parameter> injectParams = findInjectParams(testClass);

        if (configuration.componentClasses.isEmpty()) {
            throw new IllegalStateException("No component classes to test");
        }

        // Make sure Arc is down
        try {
            Arc.shutdown();
        } catch (Exception e) {
            throw new IllegalStateException("An error occured during ArC shutdown: " + e);
        }

        // Build index
        IndexView index;
        try {
            Indexer indexer = new Indexer();
            for (Class<?> componentClass : configuration.componentClasses) {
                // Make sure that component hierarchy and all annotations present are indexed
                indexComponentClass(indexer, componentClass);
            }
            indexer.indexClass(ConfigProperty.class);
            index = BeanArchives.buildImmutableBeanArchiveIndex(indexer.complete());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

        ClassLoader testClassClassLoader = testClass.getClassLoader();
        // The test class is loaded by the QuarkusClassLoader in continuous testing environment
        boolean isContinuousTesting = testClassClassLoader instanceof QuarkusClassLoader;
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();

        IndexView computingIndex = BeanArchives.buildComputingBeanArchiveIndex(oldTccl,
                new ConcurrentHashMap<>(), index);

        try {

            // These are populated after BeanProcessor.registerCustomContexts() is called
            List<DotName> qualifiers = new ArrayList<>();
            Set<String> interceptorBindings = new HashSet<>();
            AtomicReference<BeanResolver> beanResolver = new AtomicReference<>();

            BeanProcessor.Builder builder = BeanProcessor.builder()
                    .setName(testClass.getName().replace('.', '_'))
                    .addRemovalExclusion(b -> {
                        // Do not remove beans:
                        // 1. Injected in the test class or in a test method parameter
                        // 2. Annotated with @Unremovable
                        if (b.getTarget().isPresent()
                                && b.getTarget().get().hasDeclaredAnnotation(Unremovable.class)) {
                            return true;
                        }
                        for (Field injectionPoint : injectFields) {
                            if (beanResolver.get().matches(b, Types.jandexType(injectionPoint.getGenericType()),
                                    getQualifiers(injectionPoint, qualifiers))) {
                                return true;
                            }
                        }
                        for (Parameter param : injectParams) {
                            if (beanResolver.get().matches(b, Types.jandexType(param.getParameterizedType()),
                                    getQualifiers(param, qualifiers))) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .setImmutableBeanArchiveIndex(index)
                    .setComputingBeanArchiveIndex(computingIndex)
                    .setRemoveUnusedBeans(true);

            // We need collect all generated resources so that we can remove them after the test
            // NOTE: previously we kept the generated framework classes (to speedup subsequent test runs) but that breaks the existing @QuarkusTests
            Set<Path> generatedResources;

            // E.g. target/generated-arc-sources/org/acme/ComponentsProvider
            File componentsProviderFile = getComponentsProviderFile(testClass);

            if (isContinuousTesting) {
                generatedResources = Set.of();
                Map<String, byte[]> classes = new HashMap<>();
                builder.setOutput(new ResourceOutput() {
                    @Override
                    public void writeResource(Resource resource) throws IOException {
                        switch (resource.getType()) {
                            case JAVA_CLASS:
                                classes.put(resource.getName() + ".class", resource.getData());
                                ((QuarkusClassLoader) testClass.getClassLoader()).reset(classes, Map.of());
                                break;
                            case SERVICE_PROVIDER:
                                if (resource.getName()
                                        .endsWith(ComponentsProvider.class.getName())) {
                                    componentsProviderFile.getParentFile()
                                            .mkdirs();
                                    try (FileOutputStream out = new FileOutputStream(componentsProviderFile)) {
                                        out.write(resource.getData());
                                    }
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported resource type: " + resource.getType());
                        }
                    }
                });
            } else {
                generatedResources = new HashSet<>();
                File testOutputDirectory = getTestOutputDirectory(testClass);
                builder.setOutput(new ResourceOutput() {
                    @Override
                    public void writeResource(Resource resource) throws IOException {
                        switch (resource.getType()) {
                            case JAVA_CLASS:
                                generatedResources.add(resource.writeTo(testOutputDirectory).toPath());
                                break;
                            case SERVICE_PROVIDER:
                                if (resource.getName()
                                        .endsWith(ComponentsProvider.class.getName())) {
                                    componentsProviderFile.getParentFile()
                                            .mkdirs();
                                    try (FileOutputStream out = new FileOutputStream(componentsProviderFile)) {
                                        out.write(resource.getData());
                                    }
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported resource type: " + resource.getType());
                        }
                    }
                });
            }

            extensionContext.getRoot().getStore(NAMESPACE).put(KEY_GENERATED_RESOURCES, generatedResources);

            builder.addAnnotationTransformer(AnnotationsTransformer.appliedToField().whenContainsAny(qualifiers)
                    .whenContainsNone(DotName.createSimple(Inject.class)).thenTransform(t -> t.add(Inject.class)));

            builder.addAnnotationTransformer(new JaxrsSingletonTransformer());
            for (AnnotationsTransformer transformer : configuration.annotationsTransformers) {
                builder.addAnnotationTransformer(transformer);
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
                    Map<String, Set<String>> prefixToConfigMappings = new HashMap<>();
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
                                    Set<String> mappingClasses = prefixToConfigMappings.computeIfAbsent(prefix,
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

                    if (!prefixToConfigMappings.isEmpty()) {
                        Set<ConfigClassWithPrefix> configMappings = new HashSet<>();
                        for (Entry<String, Set<String>> e : prefixToConfigMappings.entrySet()) {
                            for (String mapping : e.getValue()) {
                                DotName mappingName = DotName.createSimple(mapping);
                                registrationContext.configure(mappingName)
                                        .addType(mappingName)
                                        .creator(ConfigMappingBeanCreator.class)
                                        .param("mappingClass", mapping)
                                        .param("prefix", e.getKey())
                                        .done();
                                configMappings.add(ConfigClassWithPrefix
                                        .configClassWithPrefix(ConfigMappingBeanCreator.tryLoad(mapping), e.getKey()));
                            }
                        }
                        extensionContext.getRoot().getStore(NAMESPACE).put(KEY_CONFIG_MAPPINGS, configMappings);
                    }

                    LOG.debugf("Test injection points analyzed in %s ms [found: %s, mocked: %s]",
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                            registrationContext.getInjectionPoints().size(),
                            unsatisfiedInjectionPoints.size());

                    // Find all methods annotated with interceptor annotations and register them as synthetic interceptors
                    processTestInterceptorMethods(testClass, extensionContext, registrationContext, interceptorBindings);
                }
            });

            // Register mock beans
            for (MockBeanConfiguratorImpl<?> mockConfigurator : configuration.mockConfigurators) {
                builder.addBeanRegistrar(registrarForMock(mockConfigurator));
            }

            // Process the deployment
            BeanProcessor beanProcessor = builder.build();
            try {
                Consumer<BytecodeTransformer> unsupportedBytecodeTransformer = new Consumer<BytecodeTransformer>() {
                    @Override
                    public void accept(BytecodeTransformer transformer) {
                        throw new UnsupportedOperationException();
                    }
                };
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
                beanProcessor.initialize(unsupportedBytecodeTransformer, Collections.emptyList());
                ValidationContext validationContext = beanProcessor.validate(unsupportedBytecodeTransformer);
                beanProcessor.processValidationErrors(validationContext);
                // Generate resources in parallel
                ExecutorService executor = Executors.newCachedThreadPool();
                beanProcessor.generateResources(null, new HashSet<>(), unsupportedBytecodeTransformer, true, executor);
                executor.shutdown();
            } catch (IOException e) {
                throw new IllegalStateException("Error generating resources", e);
            }

            // Use a custom ClassLoader to load the generated ComponentsProvider file
            // In continuous testing the CL that loaded the test class must be used as the parent CL
            QuarkusComponentTestClassLoader testClassLoader = new QuarkusComponentTestClassLoader(
                    isContinuousTesting ? testClassClassLoader : oldTccl,
                    componentsProviderFile,
                    null);
            Thread.currentThread().setContextClassLoader(testClassLoader);

        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        return oldTccl;
    }

    private void processTestInterceptorMethods(Class<?> testClass, ExtensionContext extensionContext,
            BeanRegistrar.RegistrationContext registrationContext, Set<String> interceptorBindings) {
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
            InterceptorMethodCreator.registerCreate(key, ctx -> {
                return ic -> {
                    Object instance = null;
                    if (!Modifier.isStatic(method.getModifiers())) {
                        Object testInstance = extensionContext.getRoot().getStore(NAMESPACE).get(KEY_TEST_INSTANCE);
                        if (testInstance == null) {
                            throw new IllegalStateException("Test instance not available");
                        }
                        instance = testInstance;
                        if (!method.canAccess(instance)) {
                            method.setAccessible(true);
                        }
                    }
                    return method.invoke(instance, ic);
                };
            });
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

    private void validateTestInterceptorMethod(Method method) {
        Parameter[] params = method.getParameters();
        if (params.length != 1 || !InvocationContext.class.isAssignableFrom(params[0].getType())) {
            throw new IllegalStateException("A test interceptor method must declare exactly one InvocationContext parameter:"
                    + Arrays.toString(params));
        }

    }

    private Set<Annotation> findBindings(Method method, Set<String> bindings) {
        return Arrays.stream(method.getAnnotations()).filter(a -> bindings.contains(a.annotationType().getName()))
                .collect(Collectors.toSet());
    }

    private void indexComponentClass(Indexer indexer, Class<?> componentClass) {
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

    private void indexAnnotatedElement(Indexer indexer, AnnotatedElement element) throws IOException {
        for (Annotation annotation : element.getAnnotations()) {
            indexer.indexClass(annotation.annotationType());
        }
    }

    private boolean isSatisfied(Type requiredType, Set<AnnotationInstance> qualifiers, InjectionPointInfo injectionPoint,
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

    private String nameToPath(String name) {
        return name.replace('.', File.separatorChar);
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }

    private List<FieldInjector> injectFields(Class<?> testClass, Object testInstance) throws Exception {
        List<FieldInjector> injectedFields = new ArrayList<>();
        for (Field field : findInjectFields(testClass)) {
            injectedFields.add(new FieldInjector(field, testInstance));
        }
        return injectedFields;
    }

    private List<Field> findInjectFields(Class<?> testClass) {
        List<Class<? extends Annotation>> injectAnnotations;
        Class<? extends Annotation> deprecatedInjectMock = loadDeprecatedInjectMock();
        if (deprecatedInjectMock != null) {
            injectAnnotations = List.of(Inject.class, InjectMock.class, deprecatedInjectMock);
        } else {
            injectAnnotations = List.of(Inject.class, InjectMock.class);
        }
        return findFields(testClass, injectAnnotations);
    }

    private List<Parameter> findInjectParams(Class<?> testClass) {
        List<Method> testMethods = findMethods(testClass, m -> m.isAnnotationPresent(Test.class));
        List<Parameter> ret = new ArrayList<>();
        for (Method method : testMethods) {
            for (Parameter param : method.getParameters()) {
                if (BUILTIN_PARAMETER.test(param)
                        || param.isAnnotationPresent(InjectMock.class)
                        || param.isAnnotationPresent(SkipInject.class)) {
                    continue;
                }
                ret.add(param);
            }
        }
        return ret;
    }

    private List<Parameter> findInjectMockParams(Class<?> testClass) {
        List<Method> testMethods = findMethods(testClass, m -> m.isAnnotationPresent(Test.class));
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

    private List<Field> findFields(Class<?> testClass, List<Class<? extends Annotation>> annotations) {
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

    private List<Method> findMethods(Class<?> testClass, Predicate<Method> methodPredicate) {
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

    static class FieldInjector {

        private final Field field;
        private final List<InstanceHandle<?>> unsetHandles;

        public FieldInjector(Field field, Object testInstance) throws Exception {
            this.field = field;

            ArcContainer container = Arc.container();
            BeanManager beanManager = container.beanManager();
            java.lang.reflect.Type requiredType = field.getGenericType();
            Annotation[] qualifiers = getQualifiers(field, beanManager);

            Object injectedInstance;

            if (qualifiers.length > 0 && Arrays.stream(qualifiers).anyMatch(All.Literal.INSTANCE::equals)) {
                // Special handling for @Injec @All List
                if (isListRequiredType(requiredType)) {
                    unsetHandles = new ArrayList<>();
                    injectedInstance = handleListAll(requiredType, qualifiers, container, unsetHandles);
                } else {
                    throw new IllegalStateException("Invalid injection point type: " + field);
                }
            } else {
                InstanceHandle<?> handle = container.instance(requiredType, qualifiers);
                if (field.isAnnotationPresent(Inject.class)) {
                    if (handle.getBean().getKind() == io.quarkus.arc.InjectableBean.Kind.SYNTHETIC) {
                        throw new IllegalStateException(String
                                .format("The injected field %s expects a real component; but obtained: %s", field,
                                        handle.getBean()));
                    }
                } else {
                    if (!handle.isAvailable()) {
                        throw new IllegalStateException(String
                                .format("The injected field %s expects a mocked bean; but obtained null", field));
                    } else if (handle.getBean().getKind() != io.quarkus.arc.InjectableBean.Kind.SYNTHETIC) {
                        throw new IllegalStateException(String
                                .format("The injected field %s expects a mocked bean; but obtained: %s", field,
                                        handle.getBean()));
                    }
                }
                injectedInstance = handle.get();
                unsetHandles = List.of(handle);
            }

            if (!field.canAccess(testInstance)) {
                field.setAccessible(true);
            }

            field.set(testInstance, injectedInstance);
        }

        void unset(Object testInstance) throws Exception {
            for (InstanceHandle<?> handle : unsetHandles) {
                if (handle.getBean() != null && handle.getBean().getScope().equals(Dependent.class)) {
                    try {
                        handle.destroy();
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to destroy the injected %s", handle.getBean());
                    }
                }
            }
            field.set(testInstance, null);
        }

    }

    private static Object handleListAll(java.lang.reflect.Type requiredType, Annotation[] qualifiers, ArcContainer container,
            Collection<InstanceHandle<?>> cleanupHandles) {
        // Remove @All and add @Default if empty
        Set<Annotation> qualifiersSet = new HashSet<>();
        Collections.addAll(qualifiersSet, qualifiers);
        qualifiersSet.remove(All.Literal.INSTANCE);
        if (qualifiersSet.isEmpty()) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            qualifiers = qualifiersSet.toArray(new Annotation[] {});
        }
        List<InstanceHandle<Object>> handles = container.listAll(getListRequiredType(requiredType), qualifiers);
        cleanupHandles.addAll(handles);
        return isTypeArgumentInstanceHandle(requiredType) ? handles
                : handles.stream().map(InstanceHandle::get).collect(Collectors.toUnmodifiableList());
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadDeprecatedInjectMock() {
        try {
            return (Class<? extends Annotation>) Class.forName("io.quarkus.test.junit.mockito.InjectMock");
        } catch (Throwable e) {
            return null;
        }
    }

    private static boolean isListRequiredType(java.lang.reflect.Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return List.class.equals(parameterizedType.getRawType());
        }
        return false;
    }

    private static java.lang.reflect.Type getListRequiredType(java.lang.reflect.Type requiredType) {
        if (requiredType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) requiredType;
            if (List.class.equals(parameterizedType.getRawType())) {
                // List<String> -> String
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return null;
    }

    private static boolean isTypeArgumentInstanceHandle(java.lang.reflect.Type type) {
        // List<String> -> String
        java.lang.reflect.Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (typeArgument instanceof ParameterizedType) {
            return ((ParameterizedType) typeArgument).getRawType().equals(InstanceHandle.class);
        }
        return false;
    }

    private File getTestOutputDirectory(Class<?> testClass) {
        String outputDirectory = System.getProperty(QUARKUS_TEST_COMPONENT_OUTPUT_DIRECTORY);
        File testOutputDirectory;
        if (outputDirectory != null) {
            testOutputDirectory = new File(outputDirectory);
        } else {
            // org.acme.Foo -> org/acme/Foo.class
            String testClassResourceName = testClass.getName().replace('.', '/') + ".class";
            // org/acme/Foo.class -> /some/path/to/project/target/test-classes/org/acme/Foo.class
            String testPath = testClass.getClassLoader().getResource(testClassResourceName).getFile();
            // /some/path/to/project/target/test-classes/org/acme/Foo.class -> /some/path/to/project/target/test-classes
            String testClassesRootPath = testPath.substring(0, testPath.length() - testClassResourceName.length());
            testOutputDirectory = new File(testClassesRootPath);
        }
        if (!testOutputDirectory.canWrite()) {
            throw new IllegalStateException("Invalid test output directory: " + testOutputDirectory);
        }
        return testOutputDirectory;
    }

    private File getComponentsProviderFile(Class<?> testClass) {
        File generatedSourcesDirectory;
        File targetDir = new File("target");
        if (targetDir.canWrite()) {
            // maven build
            generatedSourcesDirectory = new File(targetDir, "generated-arc-sources");
        } else {
            File buildDir = new File("build");
            if (buildDir.canWrite()) {
                // gradle build
                generatedSourcesDirectory = new File(buildDir, "generated-arc-sources");
            } else {
                generatedSourcesDirectory = new File("quarkus-component-test/generated-arc-sources");
            }
        }
        return new File(new File(generatedSourcesDirectory, nameToPath(testClass.getPackage().getName())),
                ComponentsProvider.class.getSimpleName());
    }

}
