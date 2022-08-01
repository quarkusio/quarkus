package io.quarkus.arc.test.interceptors.arcInvContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that {@link io.quarkus.arc.ArcInvocationContext} can used as interceptor method parameter.
 */
public class ArcInvocationContextTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Foo.class, SomeBinding.class,
            ArcContextInterceptor.class, ArcContextInterceptorPrivate.class, ArcContextLifecycleInterceptor.class,
            ArcContextLifecycleInterceptorPrivate.class);

    @Test
    public void testArcContextCanBeUsedAsParam() {
        ArcContainer arc = Arc.container();

        Assertions.assertFalse(ArcContextLifecycleInterceptor.POST_CONSTRUCT_INVOKED);
        Assertions.assertFalse(ArcContextLifecycleInterceptor.PRE_DESTROY_INVOKED);
        Assertions.assertFalse(ArcContextLifecycleInterceptorPrivate.POST_CONSTRUCT_INVOKED);
        Assertions.assertFalse(ArcContextLifecycleInterceptorPrivate.PRE_DESTROY_INVOKED);

        InstanceHandle<Foo> handle = arc.instance(Foo.class);
        Foo bean = handle.get();
        String expected = Foo.class.getSimpleName() + ArcContextInterceptorPrivate.class.getSimpleName()
                + ArcContextInterceptor.class.getSimpleName();
        Assertions.assertEquals(expected, bean.ping());
        Assertions.assertTrue(ArcContextLifecycleInterceptor.POST_CONSTRUCT_INVOKED);
        Assertions.assertTrue(ArcContextLifecycleInterceptorPrivate.POST_CONSTRUCT_INVOKED);
        handle.destroy();
        Assertions.assertTrue(ArcContextLifecycleInterceptor.PRE_DESTROY_INVOKED);
        Assertions.assertTrue(ArcContextLifecycleInterceptorPrivate.PRE_DESTROY_INVOKED);
    }
}
