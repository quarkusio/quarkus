package io.quarkus.arc.test.interceptors.postConstruct.inherited;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.postConstruct.inherited.subpackage.AlternativeBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PackagePrivatePostConstructInheritanceTest {

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
        // post construct should be invoked via during creation of AlternativeBean even though it's pack-private
        Assertions.assertEquals(1, OriginalBean.TIMES_INVOKED);
    }
}
