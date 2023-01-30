package io.quarkus.arc.test.lifecyclecallbacks.inherited;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.lifecyclecallbacks.inherited.subpackage.AlternativeBean;

public class PackagePrivateCallbackInheritanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(AlternativeBean.class, OriginalBean.class);

    @Test
    public void testCallbacks() {
        ArcContainer container = Arc.container();
        InstanceHandle<OriginalBean> origBeanInstance = container.instance(OriginalBean.class);
        InstanceHandle<AlternativeBean> alternativeBeanInstance = container.instance(AlternativeBean.class);

        assertTrue(origBeanInstance.isAvailable());
        assertTrue(alternativeBeanInstance.isAvailable());

        // AlternativeBean overrides the OriginalBean
        assertEquals(origBeanInstance.getBean(), alternativeBeanInstance.getBean());
        assertEquals(AlternativeBean.class.getSimpleName(), alternativeBeanInstance.get().ping());
        assertTrue(origBeanInstance.get().ping().equals(alternativeBeanInstance.get().ping()));

        // post construct should be invoked via during creation of AlternativeBean even though it's pack-private
        assertTrue(OriginalBean.POST_CONSTRUCT.get());

        alternativeBeanInstance.destroy();

        // pre destroy should be invoked even though it's package-private and AlternativeBean lives in a different package
        assertTrue(OriginalBean.PRE_DESTROY.get());
    }
}
