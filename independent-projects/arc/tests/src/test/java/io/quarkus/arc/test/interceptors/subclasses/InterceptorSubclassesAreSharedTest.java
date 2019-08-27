package io.quarkus.arc.test.interceptors.subclasses;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that interceptor instances are shared per bean.
 * That means that bean having two methods intercepted by one interceptor will only hold reference to one interceptor
 * instance.
 */
public class InterceptorSubclassesAreSharedTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, WeirdInterceptor.class, SomeBean.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();

        SomeBean bean = arc.instance(SomeBean.class).get();
        String resultOfFirstInterception = bean.bar();
        String resultOfSecondInterception = bean.foo();
        assertEquals(2, WeirdInterceptor.timesInvoked);
        assertEquals(resultOfFirstInterception, resultOfSecondInterception);
    }
}
