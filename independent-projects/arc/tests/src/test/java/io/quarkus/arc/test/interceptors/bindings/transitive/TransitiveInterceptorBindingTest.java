package io.quarkus.arc.test.interceptors.bindings.transitive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class TransitiveInterceptorBindingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(CounterInterceptor.class, SomeAnnotation.class,
            CounterBinding.class, MethodLevelInterceptedBean.class, ClassLevelInterceptedBean.class,
            TwoLevelsDeepClassLevelInterceptedBean.class, AnotherAnnotation.class, NotABinding.class,
            NotInterceptedBean.class, TransitiveCounterInterceptor.class);

    @Test
    public void testInterceptorsAreInvoked() {
        assertTrue(Arc.container().instance(MethodLevelInterceptedBean.class).isAvailable());
        assertTrue(Arc.container().instance(ClassLevelInterceptedBean.class).isAvailable());
        assertTrue(Arc.container().instance(TwoLevelsDeepClassLevelInterceptedBean.class).isAvailable());
        assertTrue(Arc.container().instance(NotInterceptedBean.class).isAvailable());
        MethodLevelInterceptedBean methodLevelInterceptedBean = Arc.container().instance(MethodLevelInterceptedBean.class)
                .get();
        ClassLevelInterceptedBean classLevelInterceptedBean = Arc.container().instance(ClassLevelInterceptedBean.class).get();
        TwoLevelsDeepClassLevelInterceptedBean deeperHierarchyBean = Arc.container()
                .instance(TwoLevelsDeepClassLevelInterceptedBean.class).get();
        NotInterceptedBean notIntercepted = Arc.container().instance(NotInterceptedBean.class).get();

        assertEquals(0, CounterInterceptor.timesInvoked);
        assertEquals(0, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(); // empty
        methodLevelInterceptedBean.oneLevelDeepBinding();
        assertEquals(1, CounterInterceptor.timesInvoked);
        assertEquals(1, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(SomeAnnotation.class, CounterBinding.class);
        methodLevelInterceptedBean.twoLevelsDeepBinding();
        assertEquals(2, CounterInterceptor.timesInvoked);
        assertEquals(2, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(AnotherAnnotation.class, SomeAnnotation.class, CounterBinding.class);
        classLevelInterceptedBean.ping();
        assertEquals(3, CounterInterceptor.timesInvoked);
        assertEquals(3, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(SomeAnnotation.class, CounterBinding.class);
        deeperHierarchyBean.ping();
        assertEquals(4, CounterInterceptor.timesInvoked);
        assertEquals(4, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(AnotherAnnotation.class, SomeAnnotation.class, CounterBinding.class);
        CounterInterceptor.lastBindings = new HashSet<>();
        TransitiveCounterInterceptor.lastBindings = new HashSet<>();
        // following two invocations use @NotABinding which should not trigger interception
        notIntercepted.ping();
        assertEquals(4, CounterInterceptor.timesInvoked);
        assertEquals(4, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(); // empty
        methodLevelInterceptedBean.shouldNotBeIntercepted();
        assertEquals(4, CounterInterceptor.timesInvoked);
        assertEquals(4, TransitiveCounterInterceptor.timesInvoked);
        assertBindings(); // empty
    }

    @SafeVarargs
    static void assertBindings(Class<? extends Annotation>... bindings) {
        assertBindings(CounterInterceptor.lastBindings, bindings);
        assertBindings(TransitiveCounterInterceptor.lastBindings, bindings);
    }

    private static void assertBindings(Set<Annotation> actualBindings, Class<? extends Annotation>[] expectedBindings) {
        assertNotNull(actualBindings);
        assertEquals(expectedBindings.length, actualBindings.size());
        for (Class<? extends Annotation> expectedBinding : expectedBindings) {
            assertTrue(actualBindings.stream().anyMatch(it -> it.annotationType() == expectedBinding));
        }
    }
}
