package io.quarkus.arc.test.unproxyable;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;

public class RequestScopedFinalMethodsTest {

    @RegisterExtension
    public static QuarkusUnitTest container = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RequestScopedBean.class, OtherRequestScopedBean.class, OtherRequestScopeBeanProducer.class));

    @Test
    public void testRequestScopedBeanWorksProperly() {
        ArcContainer container = Arc.container();
        ManagedContext requestContext = container.requestContext();
        requestContext.activate();

        InstanceHandle<RequestScopedBean> handle = container.instance(RequestScopedBean.class);
        Assertions.assertTrue(handle.isAvailable());
        InstanceHandle<OtherRequestScopedBean> otherHandle = container.instance(OtherRequestScopedBean.class);
        Assertions.assertTrue(otherHandle.isAvailable());

        RequestScopedBean bean = handle.get();
        Assertions.assertNull(bean.getProp());
        bean.setProp(100);
        Assertions.assertEquals(100, bean.getProp());

        OtherRequestScopedBean otherBean = otherHandle.get();
        Assertions.assertNull(otherBean.getProp());
        otherBean.setProp(100);
        Assertions.assertEquals(100, otherBean.getProp());

        requestContext.terminate();
        requestContext.activate();

        handle = container.instance(RequestScopedBean.class);
        Assertions.assertTrue(handle.isAvailable());
        bean = handle.get();
        Assertions.assertNull(bean.getProp());

        otherHandle = container.instance(OtherRequestScopedBean.class);
        Assertions.assertTrue(otherHandle.isAvailable());
        otherBean = otherHandle.get();
        Assertions.assertNull(otherBean.getProp());
    }

    @RequestScoped
    @Unremovable
    static class RequestScopedBean {
        private Integer prop = null;

        public final Integer getProp() {
            return prop;
        }

        public final void setProp(Integer prop) {
            this.prop = prop;
        }
    }

    static class OtherRequestScopedBean {
        private Integer prop = null;

        public final Integer getProp() {
            return prop;
        }

        public final void setProp(Integer prop) {
            this.prop = prop;
        }
    }

    @Dependent
    static class OtherRequestScopeBeanProducer {

        @RequestScoped
        @Unremovable
        public OtherRequestScopedBean produce() {
            return new OtherRequestScopedBean();
        }
    }

}
