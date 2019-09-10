package io.quarkus.arquillian;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.event.suite.After;

public class CreationalContextDestroyer {

    @Inject
    private Instance<CreationalContext> creationalContext;

    public void destroy(@Observes EventContext<After> event) {
        try {
            event.proceed();
        } finally {
            CreationalContext<Object> cc = creationalContext.get();
            if (cc != null) {
                cc.release();
            }
        }
    }

}
