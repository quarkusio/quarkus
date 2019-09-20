package io.quarkus.arc.test.interceptors.bindings.transitive;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TransitiveInterceptorBindingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(CounterInterceptor.class, SomeAnnotation.class,
            CounterBinding.class, MethodLevelInterceptedBean.class, ClassLevelInterceptedBean.class,
            TwoLevelsDeepClassLevelInterceptedBean.class, AnotherAnnotation.class, NotABinding.class,
            NotInterceptedBean.class, TransitiveCounterInterceptor.class);

    @Test
    public void testInterceptorsAreInvoked() {
        Assertions.assertTrue(Arc.container().instance(MethodLevelInterceptedBean.class).isAvailable());
        Assertions.assertTrue(Arc.container().instance(ClassLevelInterceptedBean.class).isAvailable());
        Assertions.assertTrue(Arc.container().instance(TwoLevelsDeepClassLevelInterceptedBean.class).isAvailable());
        Assertions.assertTrue(Arc.container().instance(NotInterceptedBean.class).isAvailable());
        MethodLevelInterceptedBean methodLevelInterceptedBean = Arc.container().instance(MethodLevelInterceptedBean.class)
                .get();
        ClassLevelInterceptedBean classLevelInterceptedBean = Arc.container().instance(ClassLevelInterceptedBean.class).get();
        TwoLevelsDeepClassLevelInterceptedBean deeperHierarchyBean = Arc.container()
                .instance(TwoLevelsDeepClassLevelInterceptedBean.class).get();
        NotInterceptedBean notIntercepted = Arc.container().instance(NotInterceptedBean.class).get();

        Assertions.assertTrue(CounterInterceptor.timesInvoked == 0);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 0);
        methodLevelInterceptedBean.oneLevelDeepBinding();
        Assertions.assertTrue(CounterInterceptor.timesInvoked == 1);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 1);
        methodLevelInterceptedBean.twoLevelsDeepBinding();
        Assertions.assertTrue(CounterInterceptor.timesInvoked == 2);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 2);
        classLevelInterceptedBean.ping();
        Assertions.assertTrue(CounterInterceptor.timesInvoked == 3);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 3);
        deeperHierarchyBean.ping();
        Assertions.assertTrue(CounterInterceptor.timesInvoked == 4);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 4);
        // following two invocations use @NotABinding which should not trigger interception
        notIntercepted.ping();
        Assertions.assertTrue(CounterInterceptor.timesInvoked == 4);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 4);
        methodLevelInterceptedBean.shouldNotBeIntercepted();
        Assertions.assertTrue(CounterInterceptor.timesInvoked == 4);
        Assertions.assertTrue(TransitiveCounterInterceptor.timesInvoked == 4);
    }

}
