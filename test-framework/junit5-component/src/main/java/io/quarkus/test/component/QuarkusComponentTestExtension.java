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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
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
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

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
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * JUnit extension that makes it easy to test Quarkus components, aka the CDI beans.
 *
 * <h2>Lifecycle</h2>
 * <p>
 * The CDI container is started and a dedicated SmallRyeConfig is registered during the {@code before all} test phase. The
 * container is stopped and the config is released during the {@code after all} test phase. The fields annotated with
 * {@code jakarta.inject.Inject} are injected after a test instance is created and unset before a test instance is destroyed.
 * Moreover, the dependent beans injected into fields annotated with {@code jakarta.inject.Inject} are correctly destroyed
 * before a test instance is destroyed. Finally, the CDI request context is activated and terminated per
 * each test method.
 *
 * <h2>Auto Mocking Unsatisfied Dependencies</h2>
 * <p>
 * Unlike in regular CDI environments the test does not fail if a component injects an unsatisfied dependency. Instead, a
 * synthetic bean is registered automatically for each combination of required type and qualifiers of an injection point that
 * resolves to an unsatisfied dependency. The bean has the {@link Singleton} scope so it's shared across all injection points
 * with the same required type and qualifiers. The injected reference is an unconfigured Mockito mock. You can inject the mock
 * in your test and leverage the Mockito API to configure the behavior.
 *
 * <h2>Custom Mocks For Unsatisfied Dependencies</h2>
 * <p>
 * Sometimes you need the full control over the bean attributes and maybe even configure the default mock behavior. You can use
 * the mock configurator API via the {@link #mock(Class)} method.
 *
 * <h2>Configuration</h2>
 * <p>
 * A dedicated {@link SmallRyeConfig} is registered during the {@code before all} test phase. Moreover, it's possible to set the
 * configuration properties via the {@link #configProperty(String, String)} method. If you only need to use the default values
 * for missing config properties, then the {@link #useDefaultConfigProperties()}
 * might come in useful.
 *
 * @see InjectMock
 * @see TestConfigProperty
 */
@Experimental("This feature is experimental and the API may change in the future")
public class QuarkusComponentTestExtension
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, TestInstancePostProcessor,
        TestInstancePreDestroyCallback, ConfigSource {

    private static final Logger LOG = Logger.getLogger(QuarkusComponentTestExtension.class);

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(QuarkusComponentTestExtension.class);

    // Strings used as keys in ExtensionContext.Store
    private static final String KEY_OLD_TCCL = "oldTccl";
    private static final String KEY_OLD_CONFIG_PROVIDER_RESOLVER = "oldConfigProviderResolver";
    private static final String KEY_GENERATED_RESOURCES = "generatedResources";
    private static final String KEY_INJECTED_FIELDS = "injectedFields";
    private static final String KEY_TEST_INSTANCE = "testInstance";
    private static final String KEY_CONFIG = "config";

    private static final String QUARKUS_TEST_COMPONENT_OUTPUT_DIRECTORY = "quarkus.test.component.output-directory";

    private final Map<String, String> configProperties;
    private final List<Class<?>> additionalComponentClasses;
    private final List<MockBeanConfiguratorImpl<?>> mockConfigurators;
    private final AtomicBoolean useDefaultConfigProperties = new AtomicBoolean();
    private final AtomicBoolean addNestedClassesAsComponents = new AtomicBoolean(true);

    // Used for declarative registration
    public QuarkusComponentTestExtension() {
        this.additionalComponentClasses = List.of();
        this.configProperties = new HashMap<>();
        this.mockConfigurators = new ArrayList<>();
    }

    /**
     * The initial set of components under test is derived from the test class. The types of all fields annotated with
     * {@link jakarta.inject.Inject} are considered the component types.
     *
     * @param additionalComponentClasses
     */
    public QuarkusComponentTestExtension(Class<?>... additionalComponentClasses) {
        this.additionalComponentClasses = List.of(additionalComponentClasses);
        this.configProperties = new HashMap<>();
        this.mockConfigurators = new ArrayList<>();
    }

    /**
     * Configure a new mock of a bean.
     * <p>
     * Note that a mock is created automatically for all unsatisfied dependencies in the test. This API provides full control
     * over the bean attributes. The default values are derived from the bean class.
     *
     * @param beanClass
     * @return a new mock bean configurator
     * @see MockBeanConfigurator#create(Function)
     */
    public <T> MockBeanConfigurator<T> mock(Class<T> beanClass) {
        return new MockBeanConfiguratorImpl<>(this, beanClass);
    }

    /**
     * Set a configuration property for the test.
     *
     * @param key
     * @param value
     * @return the extension
     */
    public QuarkusComponentTestExtension configProperty(String key, String value) {
        this.configProperties.put(key, value);
        return this;
    }

    /**
     * Use the default values for missing config properties. By default, a missing config property results in a test failure.
     * <p>
     * For primitives the default values as defined in the JLS are used. For any other type {@code null} is injected.
     *
     * @return the extension
     */
    public QuarkusComponentTestExtension useDefaultConfigProperties() {
        this.useDefaultConfigProperties.set(true);
        return this;
    }

    /**
     * Ignore the static nested classes declared on the test class.
     * <p>
     * By default, all static nested classes declared on the test class are added to the set of additional components under
     * test.
     *
     * @return the extension
     */
    public QuarkusComponentTestExtension ignoreNestedClasses() {
        this.addNestedClassesAsComponents.set(false);
        return this;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        long start = System.nanoTime();

        // Inject test class fields
        context.getRoot().getStore(NAMESPACE).put(KEY_INJECTED_FIELDS,
                injectFields(context.getRequiredTestClass(), testInstance));
        context.getRoot().getStore(NAMESPACE).put(KEY_TEST_INSTANCE, testInstance);

        LOG.debugf("postProcessTestInstance: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void preDestroyTestInstance(ExtensionContext context) throws Exception {
        long start = System.nanoTime();

        for (FieldInjector fieldInjector : (List<FieldInjector>) context.getRoot().getStore(NAMESPACE)
                .get(KEY_INJECTED_FIELDS, List.class)) {
            fieldInjector.unset(context.getRequiredTestInstance());
        }

        LOG.debugf("preDestroyTestInstance: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        long start = System.nanoTime();

        Class<?> testClass = context.getRequiredTestClass();

        // Extension may be registered declaratively
        Set<Class<?>> componentClasses = new HashSet<>(this.additionalComponentClasses);
        QuarkusComponentTest testAnnotation = testClass.getAnnotation(QuarkusComponentTest.class);
        if (testAnnotation != null) {
            Collections.addAll(componentClasses, testAnnotation.value());
            if (testAnnotation.useDefaultConfigProperties()) {
                this.useDefaultConfigProperties.set(true);
            }
            this.addNestedClassesAsComponents.set(testAnnotation.addNestedClassesAsComponents());
        }
        // All fields annotated with @Inject represent component classes
        Class<?> current = testClass;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class) && !resolvesToBuiltinBean(field.getType())) {
                    componentClasses.add(field.getType());
                }
            }
            current = current.getSuperclass();
        }
        // All static nested classes declared on the test class are components
        if (this.addNestedClassesAsComponents.get()) {
            for (Class<?> declaredClass : testClass.getDeclaredClasses()) {
                if (Modifier.isStatic(declaredClass.getModifiers())) {
                    componentClasses.add(declaredClass);
                }
            }
        }

        TestConfigProperty[] testConfigProperties = testClass.getAnnotationsByType(TestConfigProperty.class);
        for (TestConfigProperty testConfigProperty : testConfigProperties) {
            this.configProperties.put(testConfigProperty.key(), testConfigProperty.value());
        }

        ClassLoader oldTccl = initArcContainer(context, componentClasses);
        context.getRoot().getStore(NAMESPACE).put(KEY_OLD_TCCL, oldTccl);

        ConfigProviderResolver oldConfigProviderResolver = ConfigProviderResolver.instance();
        context.getRoot().getStore(NAMESPACE).put(KEY_OLD_CONFIG_PROVIDER_RESOLVER, oldConfigProviderResolver);

        SmallRyeConfigProviderResolver smallRyeConfigProviderResolver = new SmallRyeConfigProviderResolver();
        ConfigProviderResolver.setInstance(smallRyeConfigProviderResolver);

        // TCCL is now the QuarkusComponentTestClassLoader set during initialization
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        SmallRyeConfig config = new SmallRyeConfigBuilder().forClassLoader(tccl)
                .addDefaultInterceptors()
                .addDefaultSources()
                .withSources(new ApplicationPropertiesConfigSourceLoader.InFileSystem())
                .withSources(new ApplicationPropertiesConfigSourceLoader.InClassPath())
                .withSources(this)
                .build();
        smallRyeConfigProviderResolver.registerConfig(config, tccl);
        context.getRoot().getStore(NAMESPACE).put(KEY_CONFIG, config);
        ConfigBeanCreator.setClassLoader(tccl);

        LOG.debugf("beforeAll: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        long start = System.nanoTime();

        ClassLoader oldTccl = context.getRoot().getStore(NAMESPACE).get(KEY_OLD_TCCL, ClassLoader.class);
        Thread.currentThread().setContextClassLoader(oldTccl);

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

        @SuppressWarnings("unchecked")
        Set<Path> generatedResources = context.getRoot().getStore(NAMESPACE).get(KEY_GENERATED_RESOURCES, Set.class);
        for (Path path : generatedResources) {
            try {
                LOG.debugf("Delete generated %s", path);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOG.errorf("Unable to delete the generated resource %s: ", path, e.getMessage());
            }
        }

        LOG.debugf("afterAll: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        long start = System.nanoTime();

        // Activate the request context
        ArcContainer container = Arc.container();
        container.requestContext().activate();

        LOG.debugf("beforeEach: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        long start = System.nanoTime();

        // Terminate the request context
        ArcContainer container = Arc.container();
        container.requestContext().terminate();

        LOG.debugf("afterEach: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public Set<String> getPropertyNames() {
        return configProperties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return configProperties.get(propertyName);
    }

    @Override
    public String getName() {
        return QuarkusComponentTestExtension.class.getName();
    }

    @Override
    public int getOrdinal() {
        // System properties (400) and ENV variables (300) take precedence but application.properties has lower priority (250)
        return 275;
    }

    void registerMockBean(MockBeanConfiguratorImpl<?> mock) {
        this.mockConfigurators.add(mock);
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

    private static Annotation[] getQualifiers(Field field, BeanManager beanManager) {
        List<Annotation> ret = new ArrayList<>();
        Annotation[] annotations = field.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : annotations) {
            if (beanManager.isQualifier(fieldAnnotation.annotationType())) {
                ret.add(fieldAnnotation);
            }
        }
        return ret.toArray(new Annotation[0]);
    }

    private static Set<AnnotationInstance> getQualifiers(Field field, Collection<DotName> qualifiers) {
        Set<AnnotationInstance> ret = new HashSet<>();
        Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
        for (Annotation annotation : fieldAnnotations) {
            if (qualifiers.contains(DotName.createSimple(annotation.annotationType()))) {
                ret.add(Annotations.jandexAnnotation(annotation));
            }
        }
        return ret;
    }

    private ClassLoader initArcContainer(ExtensionContext extensionContext, Collection<Class<?>> componentClasses) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        // Collect all test class injection points to define a bean removal exclusion
        List<Field> testClassInjectionPoints = findInjectFields(testClass);

        if (componentClasses.isEmpty()) {
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
            for (Class<?> componentClass : componentClasses) {
                // Make sure that component hierarchy and all annotations present are indexed
                indexComponentClass(indexer, componentClass);
            }
            indexer.indexClass(ConfigProperty.class);
            index = BeanArchives.buildImmutableBeanArchiveIndex(indexer.complete());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

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
                        // 1. Injected in the test class
                        // 2. Annotated with @Unremovable
                        if (b.getTarget().isPresent()
                                && b.getTarget().get().hasDeclaredAnnotation(Unremovable.class)) {
                            return true;
                        }
                        for (Field injectionPoint : testClassInjectionPoints) {
                            if (beanResolver.get().matches(b, Types.jandexType(injectionPoint.getGenericType()),
                                    getQualifiers(injectionPoint, qualifiers))) {
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
            Set<Path> generatedResources = new HashSet<>();

            File componentsProviderFile = getComponentsProviderFile(testClass);
            if (testClass.getClassLoader() instanceof QuarkusClassLoader) {
                //continuous testing environment
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
                                throw new IllegalArgumentException();
                        }
                    }
                });
            } else {
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
                                throw new IllegalArgumentException();
                        }
                    }
                });
            }

            extensionContext.getRoot().getStore(NAMESPACE).put(KEY_GENERATED_RESOURCES, generatedResources);

            builder.addAnnotationTransformer(AnnotationsTransformer.appliedToField().whenContainsAny(qualifiers)
                    .whenContainsNone(DotName.createSimple(Inject.class)).thenTransform(t -> t.add(Inject.class)));

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

                    // Analyze injection points
                    // - find Config and @ConfigProperty injection points
                    // - find unsatisfied injection points
                    for (InjectionPointInfo injectionPoint : registrationContext.getInjectionPoints()) {
                        BuiltinBean builtin = BuiltinBean.resolve(injectionPoint);
                        if (builtin != null && builtin != BuiltinBean.INSTANCE && builtin != BuiltinBean.LIST) {
                            continue;
                        }
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
                        if (isSatisfied(requiredType, requiredQualifiers, injectionPoint, beans, beanDeployment)) {
                            continue;
                        }
                        if (requiredType.kind() == Kind.PRIMITIVE || requiredType.kind() == Kind.ARRAY) {
                            throw new IllegalStateException(
                                    "Found an unmockable unsatisfied injection point: " + injectionPoint.getTargetInfo());
                        }
                        unsatisfiedInjectionPoints.add(new TypeAndQualifiers(requiredType, requiredQualifiers));
                        LOG.debugf("Unsatisfied injection point found: %s", injectionPoint.getTargetInfo());
                    }

                    // Make sure that all @InjectMock fields are also considered unsatisfied dependencies
                    // This means that a mock is created even if no component declares this dependency
                    for (Field field : findFields(testClass, List.of(InjectMock.class))) {
                        Set<AnnotationInstance> requiredQualifiers = getQualifiers(field, qualifiers);
                        if (requiredQualifiers.isEmpty()) {
                            requiredQualifiers = Set.of(AnnotationInstance.builder(DotNames.DEFAULT).build());
                        }
                        unsatisfiedInjectionPoints
                                .add(new TypeAndQualifiers(Types.jandexType(field.getGenericType()), requiredQualifiers));
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
                                .param("useDefaultConfigProperties", useDefaultConfigProperties.get())
                                .addInjectionPoint(ClassType.create(InjectionPoint.class))
                                .creator(ConfigPropertyBeanCreator.class);
                        for (TypeAndQualifiers configPropertyInjectionPoint : configPropertyInjectionPoints) {
                            configPropertyConfigurator.addType(configPropertyInjectionPoint.type);
                        }
                        configPropertyConfigurator.done();
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
            for (MockBeanConfiguratorImpl<?> mockConfigurator : mockConfigurators) {
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
            QuarkusComponentTestClassLoader testClassLoader = new QuarkusComponentTestClassLoader(oldTccl,
                    componentsProviderFile,
                    null);
            Thread.currentThread().setContextClassLoader(testClassLoader);

            // Now we are ready to initialize Arc
            Arc.initialize();

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
        for (Method method : findMethods(testClass,
                List.of(AroundInvoke.class, PostConstruct.class, PreDestroy.class, AroundConstruct.class))) {
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
                        // ExtentionContext.getTestInstance() does not work
                        Object testInstance = extensionContext.getRoot().getStore(NAMESPACE).get(KEY_TEST_INSTANCE,
                                Object.class);
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
            Iterable<BeanInfo> beans,
            BeanDeployment beanDeployment) {
        for (BeanInfo bean : beans) {
            if (Beans.matches(bean, requiredType, qualifiers)) {
                LOG.debugf("Injection point %s satisfied by %s", injectionPoint.getTargetInfo(),
                        bean.toString());
                return true;
            }
        }
        for (MockBeanConfiguratorImpl<?> mock : mockConfigurators) {
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

    private List<Method> findMethods(Class<?> testClass, List<Class<? extends Annotation>> annotations) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Method method : current.getDeclaredMethods()) {
                for (Class<? extends Annotation> annotation : annotations) {
                    if (method.isAnnotationPresent(annotation)) {
                        methods.add(method);
                        break;
                    }
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
                    List<InstanceHandle<Object>> handles = container.listAll(requiredType, qualifiers);
                    if (isTypeArgumentInstanceHandle(requiredType)) {
                        injectedInstance = handles;
                    } else {
                        injectedInstance = handles.stream().map(InstanceHandle::get).collect(Collectors.toUnmodifiableList());
                    }
                    unsetHandles = cast(handles);
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

    private static boolean isTypeArgumentInstanceHandle(java.lang.reflect.Type type) {
        // List<String> -> String
        java.lang.reflect.Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (typeArgument instanceof ParameterizedType) {
            return ((ParameterizedType) typeArgument).getRawType().equals(InstanceHandle.class);
        }
        return false;
    }

    private boolean resolvesToBuiltinBean(Class<?> rawType) {
        return Instance.class.isAssignableFrom(rawType) || Event.class.equals(rawType) || BeanManager.class.equals(rawType);
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
            generatedSourcesDirectory = new File("target/generated-arc-sources");
        } else {
            File buildDir = new File("build");
            if (buildDir.canWrite()) {
                // gradle build
                generatedSourcesDirectory = new File("build/generated-arc-sources");
            } else {
                generatedSourcesDirectory = new File("quarkus-component-test/generated-arc-sources");
            }
        }
        return new File(generatedSourcesDirectory,
                nameToPath(testClass.getPackage().getName()) + File.pathSeparator + ComponentsProvider.class.getSimpleName());
    }

}
