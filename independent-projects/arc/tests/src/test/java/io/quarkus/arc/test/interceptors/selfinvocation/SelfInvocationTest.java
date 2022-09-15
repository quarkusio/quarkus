package io.quarkus.arc.test.interceptors.selfinvocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SelfInvocationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Nok.class, Ok.class,
            NokInterceptor.class, OkInterceptor.class, InterceptedBean.class, DependentInterceptedBean.class);

    @Test
    public void testInterception() {
        InterceptedBean bean = Arc.container().instance(InterceptedBean.class).get();
        assertEquals("NOK", bean.ping());
        assertEquals("OK", bean.ok());
        DependentInterceptedBean dependentBean = Arc.container().instance(DependentInterceptedBean.class).get();
        assertEquals("NOK", dependentBean.ping());
        assertEquals("OK", dependentBean.ok());
    }

    @ApplicationScoped
    static class InterceptedBean {

        public String ping() {
            return nok();
        }

        @Nok
        public String nok() {
            return "nokMethod";
        }

        @Ok
        public String ok() {
            return nok();
        }

    }

    @Dependent
    static class DependentInterceptedBean {

        public String ping() {
            return nok();
        }

        @Nok
        public String nok() {
            return "nokMethod";
        }

        @Ok
        public String ok() {
            return nok();
        }

    }

}
