package io.quarkus.test.junit5.virtual.internal;

import static io.quarkus.test.junit5.virtual.internal.VirtualThreadExtension._COLLECTOR_KEY;
import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.MediaType;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.ShouldPin;
import jdk.jfr.consumer.RecordedEvent;

class VirtualThreadExtensionTest {

    private VirtualThreadExtension extension;
    private TestExtensionContext extensionContext;
    private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create("loom-unit");

    @BeforeEach
    void setUp() {
        extensionContext = new TestExtensionContext();
        extension = new VirtualThreadExtension();
        extension.beforeAll(extensionContext);
    }

    @Test
    void beforeAll() {
        extension.beforeAll(extensionContext);
        assertThat(extensionContext.getStore(namespace).get(_COLLECTOR_KEY)).isNotNull();
    }

    @Test
    void afterAll() {
        extension.beforeAll(extensionContext);
        assertThatNoException().isThrownBy(() -> extension.afterAll(extensionContext));
        assertThat(extensionContext.getStore(namespace).get(_COLLECTOR_KEY)).isNotNull();
    }

    @Test
    void beforeEachShouldNotPin() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodShouldNotPin"));
        extension.beforeEach(extensionContext);
        assertThatNoException().isThrownBy(() -> extension.beforeEach(extensionContext));
    }

    @Test
    void beforeEachShouldNotPinAtMost1() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodShouldNotPinAtMost1"));
        extension.beforeEach(extensionContext);
        assertThatNoException().isThrownBy(() -> extension.beforeEach(extensionContext));
    }

    @Test
    void beforeEachNotAnnotated() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodNoAnnotation"));
        extension.beforeEach(extensionContext);
        assertThatNoException().isThrownBy(() -> extension.beforeEach(extensionContext));
    }

    @Test
    void afterEachShouldNotPin() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodShouldNotPin"));
        assertThatNoException().isThrownBy(() -> extension.afterEach(extensionContext));
    }

    @Test
    void afterEachShouldNotPinAtMost1() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodShouldNotPinAtMost1"));
        assertThatNoException().isThrownBy(() -> extension.afterEach(extensionContext));
    }

    @Test
    void afterEachShouldPinButNoEvents() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodShouldPinButDoesnt"));
        assertThatThrownBy(() -> extension.afterEach(extensionContext))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("was expected to pin the carrier thread");
    }

    @Test
    void afterEachNotAnnotated() throws NoSuchMethodException {
        extensionContext.setMethod(TestClass.class.getDeclaredMethod("methodNoAnnotation"));
        assertThatNoException().isThrownBy(() -> extension.afterEach(extensionContext));
    }

    private static class TestClass {
        @ShouldNotPin
        void methodShouldNotPin() {
        }

        @ShouldNotPin(atMost = 1)
        void methodShouldNotPinAtMost1() {
            TestPinJfrEvent.pin();
        }

        @ShouldPin
        void methodShouldPinButDoesnt() {
        }

        void methodNoAnnotation() {
        }

    }

    private static class TestExtensionContext implements ExtensionContext {
        private final TestStore store = new TestStore();
        private Method method;

        @Override
        public Optional<ExtensionContext> getParent() {
            return Optional.empty();
        }

        @Override
        public ExtensionContext getRoot() {
            return this;
        }

        @Override
        public String getUniqueId() {
            return "";
        }

        @Override
        public String getDisplayName() {
            return "unit test context";
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public Optional<AnnotatedElement> getElement() {
            return Optional.empty();
        }

        @Override
        public Optional<Class<?>> getTestClass() {
            return Optional.of(TestClass.class);
        }

        @Override
        public Class<?> getRequiredTestClass() {
            return ExtensionContext.super.getRequiredTestClass();
        }

        @Override
        public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
            return Optional.empty();
        }

        @Override
        public Optional<Object> getTestInstance() {
            return Optional.empty();
        }

        @Override
        public Object getRequiredTestInstance() {
            return ExtensionContext.super.getRequiredTestInstance();
        }

        @Override
        public Optional<TestInstances> getTestInstances() {
            return Optional.empty();
        }

        @Override
        public TestInstances getRequiredTestInstances() {
            return ExtensionContext.super.getRequiredTestInstances();
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        @Override
        public Optional<Method> getTestMethod() {
            return Optional.ofNullable(method);
        }

        @Override
        public Method getRequiredTestMethod() {
            return ExtensionContext.super.getRequiredTestMethod();
        }

        @Override
        public Optional<Throwable> getExecutionException() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getConfigurationParameter(String s) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> getConfigurationParameter(String key,
                Function<? super String, ? extends @Nullable T> transformer) {
            return Optional.empty();
        }

        @Override
        public void publishReportEntry(Map<String, String> map) {

        }

        @Override
        public void publishReportEntry(String key, String value) {
            ExtensionContext.super.publishReportEntry(key, value);
        }

        @Override
        public void publishReportEntry(String value) {
            ExtensionContext.super.publishReportEntry(value);
        }

        @Override
        public void publishFile(String name, org.junit.jupiter.api.MediaType mediaType, ThrowingConsumer<Path> action) {

        }

        @Override
        public TestStore getStore(Namespace namespace) {
            return store;
        }

        @Override
        public ExecutionMode getExecutionMode() {
            return ExecutionMode.SAME_THREAD;
        }

        @Override
        public ExecutableInvoker getExecutableInvoker() {
            return null;
        }

        @Override
        public List<Class<?>> getEnclosingTestClasses() {
            return List.of();
        }

        @Override
        public void publishFile(String name, MediaType mediaType, ThrowingConsumer<Path> action) {
        }

        @Override
        public void publishDirectory(String name, ThrowingConsumer<Path> action) {
        }

        @Override
        public Store getStore(StoreScope scope, Namespace namespace) {
            return null;
        }
    }

    private static class TestCollector extends Collector {
        private final List<RecordedEvent> mockEvents;

        private TestCollector(List<RecordedEvent> mockEvents) {
            this.mockEvents = mockEvents;
        }

        @Override
        public List<RecordedEvent> stop() {
            var parentEvents = super.stop();
            return mockEvents.isEmpty() ? parentEvents : mockEvents;
        }
    }

    @SuppressWarnings("unchecked")
    private static class TestStore implements ExtensionContext.Store {

        private final Map<Object, Object> store = new ConcurrentHashMap<>();
        private TestCollector testCollector;

        @Override
        public Object get(Object o) {
            return store.get(o);
        }

        public void setTestCollector(TestCollector testCollector) {
            this.testCollector = testCollector;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> V get(Object o, Class<V> aClass) {
            if (aClass.equals(TestCollector.class) && testCollector != null) {
                return (V) testCollector;
            }
            return aClass.cast(store.get(o));
        }

        @Override
        public <V> V getOrDefault(Object key, Class<V> requiredType, V defaultValue) {
            return ExtensionContext.Store.super.getOrDefault(key, requiredType, defaultValue);
        }

        @Override
        public <V> V computeIfAbsent(Class<V> type) {
            return ExtensionContext.Store.super.computeIfAbsent(type);
        }

        @Override
        public @Nullable <K, V> Object getOrComputeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator) {
            return store.computeIfAbsent(key, (Function<? super Object, ?>) defaultCreator);
        }

        @Override
        public <K, V> Object computeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator) {
            return store.computeIfAbsent(key, (Function<? super Object, ?>) defaultCreator);
        }

        @Override
        public <K, V> @Nullable V getOrComputeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator,
                Class<V> requiredType) {
            return requiredType.cast(store.computeIfAbsent(key, (Function<? super Object, ?>) defaultCreator));
        }

        @Override
        public <K, V> V computeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator, Class<V> requiredType) {
            return requiredType.cast(store.computeIfAbsent(key, (Function<? super Object, ?>) defaultCreator));
        }

        @Override
        public void put(Object o, Object o1) {
            store.put(o, o1);
        }

        @Override
        public Object remove(Object o) {
            return store.remove(o);
        }

        @Override
        public <V> V remove(Object o, Class<V> aClass) {
            return aClass.cast(store.remove(o));
        }

    }

}
