package io.quarkus.arc.test.interceptors.bindings.transitive;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class TransitiveInterceptorBindingTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(CounterInterceptor.class, SomeAnnotation.class,
            CounterBinding.class, MethodLevelInterceptedBean.class, ClassLevelInterceptedBean.class,
            TwoLevelsDeepClassLevelInterceptedBean.class, AnotherAnnotation.class, NotABinding.class,
            NotInterceptedBean.class, TransitiveCounterInterceptor.class);

    @Test
    public void testInterceptorsAreInvoked() {
        Assert.assertTrue(Arc.container().instance(MethodLevelInterceptedBean.class).isAvailable());
        Assert.assertTrue(Arc.container().instance(ClassLevelInterceptedBean.class).isAvailable());
        Assert.assertTrue(Arc.container().instance(TwoLevelsDeepClassLevelInterceptedBean.class).isAvailable());
        Assert.assertTrue(Arc.container().instance(NotInterceptedBean.class).isAvailable());
        MethodLevelInterceptedBean methodLevelInterceptedBean = Arc.container().instance(MethodLevelInterceptedBean.class)
                .get();
        ClassLevelInterceptedBean classLevelInterceptedBean = Arc.container().instance(ClassLevelInterceptedBean.class).get();
        TwoLevelsDeepClassLevelInterceptedBean deeperHierarchyBean = Arc.container()
                .instance(TwoLevelsDeepClassLevelInterceptedBean.class).get();
        NotInterceptedBean notIntercepted = Arc.container().instance(NotInterceptedBean.class).get();

        Assert.assertTrue(CounterInterceptor.timesInvoked == 0);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 0);
        methodLevelInterceptedBean.oneLevelDeepBinding();
        Assert.assertTrue(CounterInterceptor.timesInvoked == 1);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 1);
        methodLevelInterceptedBean.twoLevelsDeepBinding();
        Assert.assertTrue(CounterInterceptor.timesInvoked == 2);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 2);
        classLevelInterceptedBean.ping();
        Assert.assertTrue(CounterInterceptor.timesInvoked == 3);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 3);
        deeperHierarchyBean.ping();
        Assert.assertTrue(CounterInterceptor.timesInvoked == 4);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 4);
        // following two invocations use @NotABinding which should not trigger interception
        notIntercepted.ping();
        Assert.assertTrue(CounterInterceptor.timesInvoked == 4);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 4);
        methodLevelInterceptedBean.shouldNotBeIntercepted();
        Assert.assertTrue(CounterInterceptor.timesInvoked == 4);
        Assert.assertTrue(TransitiveCounterInterceptor.timesInvoked == 4);
    }

}
