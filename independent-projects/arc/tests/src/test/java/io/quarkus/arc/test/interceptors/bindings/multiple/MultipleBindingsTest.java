package io.quarkus.arc.test.interceptors.bindings.multiple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests behavior the case where there is more then one binding specified on an interceptor.
 */
public class MultipleBindingsTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BarBinding.class, FooBinding.class,
            MyBean.class, MyInterceptor.class, MyOtherBean.class);

    @Test
    public void testInterception() {
        assertEquals(0, MyInterceptor.TIMES_INVOKED);
        // bean only has one binding, the interceptor should not get triggered
        Arc.container().instance(MyBean.class).get().foo();
        assertEquals(0, MyInterceptor.TIMES_INVOKED);
        // bean has both bindings that the interceptor has
        Arc.container().instance(MyOtherBean.class).get().foo();
        assertEquals(1, MyInterceptor.TIMES_INVOKED);
    }
}
