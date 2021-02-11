package io.quarkus.arquillian;

import java.io.IOException;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.event.suite.After;

public class CreationalContextDestroyer {

    @Inject
    private Instance<InjectionEnricher.CreationContextHolder> creationalContext;

    public void destroy(@Observes EventContext<After> event) throws IOException {
        try {
            event.proceed();
        } finally {
            InjectionEnricher.CreationContextHolder cc = creationalContext.get();
            if (cc != null) {
                cc.close();
            }
        }
    }

}
