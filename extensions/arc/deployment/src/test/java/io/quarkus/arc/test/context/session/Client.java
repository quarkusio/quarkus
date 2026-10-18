package io.quarkus.arc.test.context.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.ActivateSessionContext;

@Dependent
class Client {

    @Inject
    SimpleBean bean;

    @ActivateSessionContext
    public String ping() {
        assertTrue(Arc.container().sessionContext().isActive());
        if (bean instanceof ClientProxy proxy) {
            assertEquals(SessionScoped.class, proxy.arc_bean().getScope());
        } else {
            fail("Not a client proxy");
        }
        return bean.ping();
    }

}
