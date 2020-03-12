package io.quarkus.arc.test.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SimpleInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Counter.class, SimpleBean.class, Simple.class,
            SimpleInterceptor.class, Logging.class,
            LoggingInterceptor.class, Lifecycle.class, LifecycleInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();

        LifecycleInterceptor.POST_CONSTRUCTS.clear();
        LifecycleInterceptor.PRE_DESTROYS.clear();
        LifecycleInterceptor.AROUND_CONSTRUCTS.clear();

        InstanceHandle<SimpleBean> handle = arc.instance(SimpleBean.class);
        SimpleBean simpleBean = handle.get();

        assertEquals(1, LifecycleInterceptor.AROUND_CONSTRUCTS.size());
        assertNotNull(LifecycleInterceptor.AROUND_CONSTRUCTS.get(0));
        assertEquals(1, LifecycleInterceptor.POST_CONSTRUCTS.size());
        assertEquals(simpleBean, LifecycleInterceptor.POST_CONSTRUCTS.get(0));

        Counter counter = arc.instance(Counter.class).get();
        LoggingInterceptor.LOG.set(null);

        assertEquals("0foo1", simpleBean.foo("0"));
        assertEquals("oof", simpleBean.bar());
        assertEquals(1, counter.get());
        assertEquals("foo", LoggingInterceptor.LOG.get());

        simpleBean.baz(42);
        assertEquals(2, counter.get());

        handle.destroy();
        assertEquals(1, LifecycleInterceptor.PRE_DESTROYS.size());
        assertEquals(simpleBean, LifecycleInterceptor.PRE_DESTROYS.get(0));
    }

}
