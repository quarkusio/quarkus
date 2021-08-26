package io.quarkus.arc.test.interceptors.complex;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultipleInterceptionTypesTogetherTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, SomeBean.class, MyInterceptor.class);

    @Test
    public void testInterceptionIsInvokedWhenAppropriate() {
        SomeBean.reset();
        MyInterceptor.reset();
        // assert initial state
        Assertions.assertEquals(false, SomeBean.postConstructInvoked.get());
        Assertions.assertEquals(false, SomeBean.preDestroyInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.aroundConstructInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.postConstructInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.preDestroyInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.aroundInvokeInvoked.get());
        // create bean instance, no method was invoked so far
        InstanceHandle<SomeBean> instance = Arc.container().instance(SomeBean.class);
        SomeBean bean = instance.get();
        // assert lifecycle callback were invoked but no around invoke was triggered
        Assertions.assertEquals(true, SomeBean.postConstructInvoked.get());
        Assertions.assertEquals(false, SomeBean.preDestroyInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.aroundConstructInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.postConstructInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.preDestroyInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.aroundInvokeInvoked.get());
        // invoke bean method and assert around invoke was triggered
        bean.ping();
        Assertions.assertEquals(true, SomeBean.postConstructInvoked.get());
        Assertions.assertEquals(false, SomeBean.preDestroyInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.aroundConstructInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.postConstructInvoked.get());
        Assertions.assertEquals(false, MyInterceptor.preDestroyInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.aroundInvokeInvoked.get());
        // trigger bean destruction and assert lifecycle interceptors were triggered
        instance.destroy();
        Assertions.assertEquals(true, SomeBean.postConstructInvoked.get());
        Assertions.assertEquals(true, SomeBean.preDestroyInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.aroundConstructInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.postConstructInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.preDestroyInvoked.get());
        Assertions.assertEquals(true, MyInterceptor.aroundInvokeInvoked.get());
    }
}
