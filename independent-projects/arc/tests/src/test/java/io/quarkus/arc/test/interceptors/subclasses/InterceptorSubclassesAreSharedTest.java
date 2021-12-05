package io.quarkus.arc.test.interceptors.subclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that interceptor instances are shared per bean.
 * That means that bean having two methods intercepted by one interceptor will only hold reference to one interceptor
 * instance.
 */
public class InterceptorSubclassesAreSharedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, WeirdInterceptor.class,
            SomeBean.class);

    @Test
    public void testInterception() throws IOException {
        ArcContainer arc = Arc.container();

        SomeBean bean = arc.instance(SomeBean.class).get();
        String resultOfFirstInterception = bean.bar();
        String resultOfSecondInterception = bean.foo();
        assertEquals(2, WeirdInterceptor.timesInvoked);
        assertEquals(resultOfFirstInterception, resultOfSecondInterception);
    }
}
