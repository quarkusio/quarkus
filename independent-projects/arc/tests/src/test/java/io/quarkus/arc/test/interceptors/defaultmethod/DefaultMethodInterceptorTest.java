package io.quarkus.arc.test.interceptors.defaultmethod;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefaultMethodInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ABinding.class, NextBinding.class, DefaultMethodBean.class,
            DefaultMethodInterface.class, MessageInterceptor.class, NextInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();

        InstanceHandle<DefaultMethodBean> handle = arc.instance(DefaultMethodBean.class);
        DefaultMethodBean simpleBean = handle.get();
        Assertions.assertEquals("intercepted:next:hello", simpleBean.hello());
        Assertions.assertEquals("intercepted:default method", simpleBean.defaultMethod());
        Assertions.assertEquals("intercepted:pong", simpleBean.ping());
    }

}
