package io.quarkus.arc.test.observers.inheritance.different.packages.subpackage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.observers.inheritance.different.packages.OriginalBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PackagePrivateObserverInheritanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(AlternativeBean.class, OriginalBean.class);

    @Test
    public void testObserverCanBeInvoked() {
        ArcContainer container = Arc.container();
        InstanceHandle<OriginalBean> origBeanInstance = container.instance(OriginalBean.class);
        InstanceHandle<AlternativeBean> alternativeBeanInstance = container.instance(AlternativeBean.class);
        Assertions.assertTrue(origBeanInstance.isAvailable());
        Assertions.assertTrue(alternativeBeanInstance.isAvailable());
        Assertions.assertTrue(origBeanInstance.get().ping().equals(alternativeBeanInstance.get().ping()));
        // the observer should be invoked twice at this point, with no exception thrown
        Assertions.assertEquals(2, OriginalBean.TIMES_INVOKED);
    }
}
