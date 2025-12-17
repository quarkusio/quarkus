package io.quarkus.test.component;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
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
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ArcInitConfig;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.EventBean;
import io.quarkus.arc.impl.InstanceImpl;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTestCallbacks.AfterStartContext;
import io.quarkus.test.component.QuarkusComponentTestCallbacks.AfterStopContext;
import io.quarkus.test.component.QuarkusComponentTestCallbacks.BeforeStartContext;
import io.quarkus.test.component.QuarkusComponentTestCallbacks.ComponentTestContext;
import io.smallrye.config.ConfigMappings.ConfigClass;
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
    static final String KEY_TEST_INSTANCE = "testInstance";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_TEST_CLASS_CONFIG = "testClassConfig";
    private static final String KEY_CONFIG_MAPPINGS = "configMappings";
    private static final String KEY_CONTAINER_STATE = "containerState";

    final QuarkusComponentTestConfiguration baseConfiguration;

    private final boolean buildShouldFail;
    private final AtomicReference<Throwable> buildFailure;

    // Used for declarative registration
    public QuarkusComponentTestExtension() {
        this(QuarkusComponentTestConfiguration.DEFAULT, false);
    }

    /**
     * The initial set of components under test is derived from the test class. The types of all fields annotated with
     * {@link jakarta.inject.Inject} are considered the component types.
     *
     * @param additionalComponentClasses
     */
    public QuarkusComponentTestExtension(Class<?>... additionalComponentClasses) {
        this(new QuarkusComponentTestConfiguration(Map.of(), Set.of(additionalComponentClasses),
                List.of(), false, true, QuarkusComponentTestExtensionBuilder.DEFAULT_CONFIG_SOURCE_ORDINAL,
                List.of(), List.of(), null, false, null), false);
    }

    QuarkusComponentTestExtension(QuarkusComponentTestConfiguration baseConfiguration, boolean startShouldFail) {
        this.baseConfiguration = baseConfiguration;
        this.buildShouldFail = startShouldFail;
        this.buildFailure = new AtomicReference<>();
    }

    boolean isBuildShouldFail() {
        return buildShouldFail;
    }

    public Throwable getBuildFailure() {
        return buildFailure.get();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        if (context.getRequiredTestClass().isAnnotationPresent(Nested.class)) {
            // There is no callback that runs after all tests in a test class but before any @Nested test classes run
            // Therefore we need to discard the existing container here
            cleanup(context);
        }
        initContainer(context);
        startContainer(context, Lifecycle.PER_CLASS);
        LOG.debugf("beforeAll: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        stopContainer(context, Lifecycle.PER_CLASS);
        cleanup(context);
        LOG.debugf("afterAll: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        startContainer(context, Lifecycle.PER_METHOD);
        if (getContainerState(context) == ContainerState.STARTED) {
            // Activate the request context
            Arc.container().requestContext().activate();
        }
        LOG.debugf("beforeEach: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        if (getContainerState(context) == ContainerState.STARTED) {
            // Terminate the request context
            Arc.container().requestContext().terminate();
            // Destroy @Dependent beans injected as test method parameters correctly
            destroyDependentTestMethodParams(context);
        }
        // Stop the container if Lifecycle.PER_METHOD is used
        stopContainer(context, Lifecycle.PER_METHOD);
        LOG.debugf("afterEach: %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        long start = System.nanoTime();
        store(context).put(KEY_TEST_INSTANCE, testInstance);
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
                && isTestMethod(parameterContext.getDeclaringExecutable())
                // A method/param annotated with @SkipInject is never supported
                && !parameterContext.isAnnotated(SkipInject.class)
                && !parameterContext.getDeclaringExecutable().isAnnotationPresent(SkipInject.class)
                // A param annotated with @org.mockito.Mock is never supported
                && !parameterContext.isAnnotated(Mock.class)
                // Skip params covered by built-in extensions
                && !BUILTIN_PARAMETER.test(parameterContext.getParameter())) {
            BeanManager beanManager = Arc.container().beanManager();
            java.lang.reflect.Type requiredType = parameterContext.getParameter().getParameterizedType();
            Annotation[] qualifiers = getQualifiers(parameterContext.getAnnotatedElement(), beanManager);
            if (isListAllInjectionPoint(requiredType, qualifiers, parameterContext.getParameter())) {
                return true;
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
        List<Object> injectedParams = store(context).get(KEY_INJECTED_PARAMS, List.class);
        ArcContainer container = Arc.container();
        BeanManager beanManager = container.beanManager();
        java.lang.reflect.Type requiredType = parameterContext.getParameter().getParameterizedType();
        Annotation[] qualifiers = getQualifiers(parameterContext.getAnnotatedElement(), beanManager);
        if (Instance.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            InstanceImpl<?> instance = InstanceImpl.forGlobalEntrypoint(getFirstActualTypeArgument(requiredType),
                    Set.of(qualifiers));
            injectedParams.add(instance);
            return instance;
        } else if (isListAllInjectionPoint(requiredType, qualifiers, parameterContext.getParameter())) {
            // Special handling for @Inject @All List<>
            Collection<InstanceHandle<?>> unsetHandles = new ArrayList<>();
            Object ret = handleListAll(requiredType, qualifiers, container, unsetHandles);
            unsetHandles.forEach(injectedParams::add);
            return ret;
        } else {
            InstanceHandle<?> handle = container.instance(requiredType, qualifiers);
            injectedParams.add(handle);
            return handle.get();
        }
    }

    private void destroyDependentTestMethodParams(ExtensionContext context) {
        @SuppressWarnings("unchecked")
        List<Object> injectedParams = store(context).get(KEY_INJECTED_PARAMS, List.class);
        for (Object param : injectedParams) {
            if (param instanceof InstanceHandle) {
                @SuppressWarnings("resource")
                InstanceHandle<?> handle = (InstanceHandle<?>) param;
                if (handle.getBean() != null && handle.getBean().getScope().equals(Dependent.class)) {
                    try {
                        handle.destroy();
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to destroy the injected %s", handle.getBean());
                    }
                }
            } else if (param instanceof InstanceImpl) {
                InstanceImpl<?> instance = (InstanceImpl<?>) param;
                instance.destroy();
            }
        }
        injectedParams.clear();
    }

    private void initContainer(ExtensionContext context) {
        if (getContainerState(context) != ContainerState.UNINITIALIZED) {
            return;
        }
        QuarkusComponentTestConfiguration testClassConfiguration = baseConfiguration
                .update(context.getRequiredTestClass());
        store(context).put(KEY_TEST_CLASS_CONFIG, testClassConfiguration);

        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        Class<?> testClass = context.getRequiredTestClass();
        ClassLoader testCl = testClass.getClassLoader();
        Thread.currentThread().setContextClassLoader(testCl);

        if (testCl instanceof QuarkusComponentTestClassLoader componentCl) {
            Map<String, Set<String>> configMappings = componentCl.getConfigMappings();
            if (!configMappings.isEmpty()) {
                Set<ConfigClass> mappings = new HashSet<>();
                for (Entry<String, Set<String>> e : configMappings.entrySet()) {
                    for (String mapping : e.getValue()) {
                        mappings.add(configClass(ConfigMappingBeanCreator.tryLoad(mapping), e.getKey()));
                    }
                }
                store(context).put(KEY_CONFIG_MAPPINGS, mappings);
            }
            try {
                InterceptorMethodCreator.register(context, componentCl.getInterceptorMethods());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to register interceptor methods", e);
            }
            buildFailure.set((Throwable) componentCl.getBuildFailure());
        }

        for (MockBeanConfiguratorImpl<?> mockBeanConfigurator : testClassConfiguration.mockConfigurators) {
            MockBeanCreator.registerCreate(testClass.getName(), cast(mockBeanConfigurator.create));
        }

        if (buildFailure.get() == null) {
            store(context).put(KEY_OLD_TCCL, oldTccl);
            setContainerState(context, ContainerState.INITIALIZED);
        } else {
            setContainerState(context, ContainerState.BUILD_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanup(ExtensionContext context) {
        if (getContainerState(context).requiresCleanup()) {
            ClassLoader oldTccl = store(context).get(KEY_OLD_TCCL, ClassLoader.class);
            if (oldTccl != null) {
                Thread.currentThread().setContextClassLoader(oldTccl);
            }
            store(context).remove(KEY_OLD_TCCL);
            store(context).remove(KEY_CONFIG_MAPPINGS);
            Set<Path> generatedResources = store(context).get(KEY_GENERATED_RESOURCES, Set.class);
            if (generatedResources != null) {
                for (Path path : generatedResources) {
                    try {
                        LOG.debugf("Delete generated %s", path);
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        LOG.errorf("Unable to delete the generated resource %s: ", path, e.getMessage());
                    }
                }
            }
            store(context).remove(KEY_GENERATED_RESOURCES);
            setContainerState(context, ContainerState.UNINITIALIZED);
        }
    }

    @SuppressWarnings("unchecked")
    private void stopContainer(ExtensionContext context, Lifecycle testInstanceLifecycle) throws Exception {
        if (testInstanceLifecycle.equals(context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD))
                && getContainerState(context) == ContainerState.STARTED) {
            for (FieldInjector fieldInjector : (List<FieldInjector>) store(context)
                    .get(KEY_INJECTED_FIELDS, List.class)) {
                fieldInjector.unset();
            }
            try {
                Arc.shutdown();
            } catch (Exception e) {
                LOG.error("An error occurred during ArC shutdown: " + e);
            }
            MockBeanCreator.clear();
            ConfigBeanCreator.clear();
            InterceptorMethodCreator.clear();
            store(context).remove(KEY_CONTAINER_STATE);

            SmallRyeConfig config = store(context).get(KEY_CONFIG, SmallRyeConfig.class);
            ConfigProviderResolver.instance().releaseConfig(config);
            ConfigProviderResolver oldConfigProviderResolver = store(context).get(KEY_OLD_CONFIG_PROVIDER_RESOLVER,
                    ConfigProviderResolver.class);
            ConfigProviderResolver.setInstance(oldConfigProviderResolver);
            setContainerState(context, ContainerState.STOPPED);

            QuarkusComponentTestConfiguration configuration = store(context).get(KEY_TEST_CLASS_CONFIG,
                    QuarkusComponentTestConfiguration.class);
            if (configuration.hasCallbacks()) {
                AfterStopContext afterStopContext = new AfterStopContextImpl(context.getRequiredTestClass());
                for (QuarkusComponentTestCallbacks callbacks : configuration.callbacks) {
                    callbacks.afterStop(afterStopContext);
                }
            }
        }
    }

    enum ContainerState {
        UNINITIALIZED,
        INITIALIZED,
        BUILD_FAILED,
        STARTED,
        STOPPED;

        boolean requiresCleanup() {
            return this == STOPPED || this == BUILD_FAILED;
        }
    }

    private ContainerState getContainerState(ExtensionContext context) {
        ContainerState state = store(context).get(KEY_CONTAINER_STATE, ContainerState.class);
        return state != null ? state : ContainerState.UNINITIALIZED;
    }

    private void setContainerState(ExtensionContext context, ContainerState state) {
        store(context).put(KEY_CONTAINER_STATE, state);
    }

    private void startContainer(ExtensionContext context, Lifecycle testInstanceLifecycle) throws Exception {
        if (!testInstanceLifecycle.equals(context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD))) {
            return;
        }
        ContainerState state = getContainerState(context);
        if (state == ContainerState.UNINITIALIZED) {
            throw new IllegalStateException("Container not initialized");
        } else if (state == ContainerState.STARTED
                // The build was expected to fail
                || state == ContainerState.BUILD_FAILED) {
            return;
        }
        // Init ArC
        Arc.initialize(ArcInitConfig.builder().setTestMode(true).build());

        QuarkusComponentTestConfiguration configuration = store(context).get(KEY_TEST_CLASS_CONFIG,
                QuarkusComponentTestConfiguration.class);
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            configuration = configuration.update(testMethod.get());
        }

        Map<String, String> configProperties;
        if (configuration.hasCallbacks()) {
            BeforeStartContextImpl beforeStartContext = new BeforeStartContextImpl(context.getRequiredTestClass(),
                    configuration.configProperties);
            for (QuarkusComponentTestCallbacks callbacks : configuration.callbacks) {
                callbacks.beforeStart(beforeStartContext);
            }
            configProperties = Map.copyOf(beforeStartContext.configProperties);
        } else {
            configProperties = configuration.configProperties;
        }

        ConfigProviderResolver oldConfigProviderResolver = ConfigProviderResolver.instance();
        store(context).put(KEY_OLD_CONFIG_PROVIDER_RESOLVER, oldConfigProviderResolver);

        SmallRyeConfigProviderResolver smallRyeConfigProviderResolver = new SmallRyeConfigProviderResolver();
        ConfigProviderResolver.setInstance(smallRyeConfigProviderResolver);

        // TCCL is now the QuarkusComponentTestClassLoader set during initialization
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        LaunchMode.set(LaunchMode.TEST);
        SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder()
                .forClassLoader(tccl)
                // Make sure the correct config profile is used
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .addDefaultInterceptors()
                .withConverters(configuration.configConverters.toArray(new Converter<?>[] {}))
                // We intentionally skip system properties and ENV variables by default
                // See https://github.com/quarkusio/quarkus/issues/48899 for more details
                .addPropertiesSources()
                .withSources(
                        new QuarkusComponentTestConfigSource(configProperties,
                                configuration.configSourceOrdinal));

        if (configuration.useSystemConfigSources) {
            configBuilder.addSystemSources();
        }

        @SuppressWarnings("unchecked")
        Set<ConfigClass> configMappings = store(context).get(KEY_CONFIG_MAPPINGS, Set.class);
        if (configMappings != null) {
            // Register the mappings found during bean discovery
            for (ConfigClass mapping : configMappings) {
                configBuilder.withMapping(mapping);
            }
        }
        if (configuration.configBuilderCustomizer != null) {
            configuration.configBuilderCustomizer.accept(configBuilder);
        }
        SmallRyeConfig config = configBuilder.build();
        smallRyeConfigProviderResolver.registerConfig(config, tccl);
        store(context).put(KEY_CONFIG, config);
        ConfigBeanCreator.setClassLoader(tccl);

        // Inject fields declared on test classes
        List<FieldInjector> injectedFields = new ArrayList<>();
        for (Object testInstance : context.getRequiredTestInstances().getAllInstances()) {
            injectedFields.addAll(injectFields(testInstance.getClass(), testInstance));
        }
        store(context).put(KEY_INJECTED_FIELDS, injectedFields);
        // Injected test method parameters
        store(context).put(KEY_INJECTED_PARAMS, new CopyOnWriteArrayList<>());
        setContainerState(context, ContainerState.STARTED);

        if (configuration.hasCallbacks()) {
            AfterStartContext afterStartContext = new AfterStartContextImpl(context.getRequiredTestClass());
            for (QuarkusComponentTestCallbacks callbacks : configuration.callbacks) {
                callbacks.afterStart(afterStartContext);
            }
        }
    }

    static Store store(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

    private static Annotation[] getQualifiers(AnnotatedElement element, BeanManager beanManager) {
        List<Annotation> ret = new ArrayList<>();
        Annotation[] annotations = element.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : annotations) {
            if (beanManager.isQualifier(fieldAnnotation.annotationType())) {
                ret.add(fieldAnnotation);
            }
        }
        if (ret.isEmpty()) {
            // Add @Default as if @InjectMock was a normal @Inject
            ret.add(Default.Literal.INSTANCE);
        }
        return ret.toArray(new Annotation[0]);
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }

    private List<FieldInjector> injectFields(Class<?> testClass, Object testInstance) throws Exception {
        List<FieldInjector> injectedFields = new ArrayList<>();
        for (Field field : findInjectFields(testClass, false)) {
            injectedFields.add(new FieldInjector(field, testInstance));
        }
        return injectedFields;
    }

    private List<Field> findInjectFields(Class<?> testClass, boolean scanEnclosingClasses) {
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
    private Class<? extends Annotation> loadInjectSpy() {
        try {
            return (Class<? extends Annotation>) Class.forName("io.quarkus.test.junit.mockito.InjectSpy");
        } catch (Throwable e) {
            return null;
        }
    }

    static boolean isTestMethod(Executable method) {
        return method.isAnnotationPresent(Test.class)
                || method.isAnnotationPresent(ParameterizedTest.class)
                || method.isAnnotationPresent(RepeatedTest.class);
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

    static class FieldInjector {

        private final Object testInstance;
        private final Field field;
        private final Runnable unsetAction;

        public FieldInjector(Field field, Object testInstance) throws Exception {
            this.field = field;
            this.testInstance = testInstance;

            ArcContainer container = Arc.container();
            BeanManager beanManager = container.beanManager();
            java.lang.reflect.Type requiredType = field.getGenericType();
            Annotation[] qualifiers = getQualifiers(field, beanManager);
            boolean isMock = field.isAnnotationPresent(InjectMock.class);
            Object injectedInstance;

            if (Instance.class.isAssignableFrom(QuarkusComponentTestConfiguration.getRawType(requiredType))) {
                InstanceImpl<?> instance = InstanceImpl.forGlobalEntrypoint(getFirstActualTypeArgument(requiredType),
                        Set.of(qualifiers));
                injectedInstance = instance;
                unsetAction = instance::destroy;
            } else if (isListAllInjectionPoint(requiredType, qualifiers, field)) {
                // Special handling for @Injec @All List
                List<InstanceHandle<?>> unsetHandles = new ArrayList<>();
                injectedInstance = handleListAll(requiredType, qualifiers, container, unsetHandles);
                unsetAction = () -> destroyDependentHandles(unsetHandles);
            } else {
                InstanceHandle<?> handle = container.instance(requiredType, qualifiers);
                InjectableBean<?> bean = handle.getBean();
                if (isMock) {
                    // @InjectMock expects a synthetic dummy mock
                    if (!handle.isAvailable()) {
                        throw new IllegalStateException(String
                                .format("The injected field [%s] expects a mocked bean; but obtained null", field));
                    } else if (bean.getKind() == InjectableBean.Kind.BUILTIN) {
                        if (!(bean instanceof EventBean)) {
                            throw new IllegalStateException(
                                    "Only the jakarta.enterprise.event.Event built-in bean can be mocked: [%s]"
                                            .formatted(field));
                        }
                    } else if (bean.getKind() != io.quarkus.arc.InjectableBean.Kind.SYNTHETIC) {
                        throw new IllegalStateException(String
                                .format("The injected field [%s] expects a mocked bean; but obtained: %s", field,
                                        handle.getBean()));
                    }
                } else {
                    // @Inject expects a real component
                    if (!handle.isAvailable()) {
                        throw new IllegalStateException(String
                                .format("The injected field [%s] expects a real component; but no matching component was registered",
                                        field,
                                        handle.getBean()));
                    } else if (bean.getKind() == io.quarkus.arc.InjectableBean.Kind.SYNTHETIC) {
                        throw new IllegalStateException(String
                                .format("The injected field [%s] expects a real component; but obtained: %s", field,
                                        handle.getBean()));
                    }
                }
                if (isMock && bean instanceof EventBean) {
                    // Event mocks require special handling
                    Event<?> mock = Mockito.mock(Event.class);
                    Object eventInstance = handle.get();
                    if (eventInstance instanceof Mockable mockable) {
                        mockable.arc$setMock(mock);
                    } else {
                        throw new IllegalStateException(
                                "%s is not a Mockable Event implementation".formatted(eventInstance.getClass()));
                    }
                    injectedInstance = mock;
                    unsetAction = () -> mockable.arc$clearMock();
                } else {
                    injectedInstance = handle.get();
                    unsetAction = () -> destroyDependentHandles(List.of(handle));
                }
            }

            if (!field.canAccess(testInstance)) {
                field.setAccessible(true);
            }

            field.set(testInstance, injectedInstance);
        }

        void unset() throws Exception {
            if (unsetAction != null) {
                unsetAction.run();
            }
            field.set(testInstance, null);
        }

        void destroyDependentHandles(List<InstanceHandle<?>> handles) {
            for (InstanceHandle<?> handle : handles) {
                if (handle.getBean() != null && handle.getBean().getScope().equals(Dependent.class)) {
                    try {
                        handle.destroy();
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to destroy the injected %s", handle.getBean());
                    }
                }
            }
        }

    }

    private static Object handleListAll(java.lang.reflect.Type requiredType, Annotation[] qualifiers, ArcContainer container,
            Collection<InstanceHandle<?>> unsetHandles) {
        // Remove @All and add @Default if empty
        Set<Annotation> qualifiersSet = new HashSet<>();
        Collections.addAll(qualifiersSet, qualifiers);
        qualifiersSet.remove(All.Literal.INSTANCE);
        if (qualifiersSet.isEmpty()) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            qualifiers = qualifiersSet.toArray(new Annotation[] {});
        }
        List<InstanceHandle<Object>> handles = container.listAll(getFirstActualTypeArgument(requiredType), qualifiers);
        unsetHandles.addAll(handles);
        return isTypeArgumentInstanceHandle(requiredType) ? handles
                : handles.stream().map(InstanceHandle::get).collect(Collectors.toUnmodifiableList());
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

    private static boolean isTypeArgumentInstanceHandle(java.lang.reflect.Type type) {
        // List<String> -> String
        java.lang.reflect.Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (typeArgument instanceof ParameterizedType) {
            return ((ParameterizedType) typeArgument).getRawType().equals(InstanceHandle.class);
        }
        return false;
    }

    static class ComponentTestContextImpl implements ComponentTestContext {

        private final Class<?> testClass;

        ComponentTestContextImpl(Class<?> testClass) {
            this.testClass = testClass;
        }

        @Override
        public Class<?> getTestClass() {
            return testClass;
        }

    }

    private static class BeforeStartContextImpl extends ComponentTestContextImpl implements BeforeStartContext {

        private final Map<String, String> configProperties;

        BeforeStartContextImpl(Class<?> testClass, Map<String, String> existingProperties) {
            super(testClass);
            this.configProperties = new HashMap<>(existingProperties);
        }

        @Override
        public void setConfigProperty(String key, String value) {
            configProperties.put(key, value);
        }

    }

    private static class AfterStartContextImpl extends ComponentTestContextImpl implements AfterStartContext {

        AfterStartContextImpl(Class<?> testClass) {
            super(testClass);
        }

    }

    private static class AfterStopContextImpl extends ComponentTestContextImpl implements AfterStopContext {

        AfterStopContextImpl(Class<?> testClass) {
            super(testClass);
        }

    }

}
