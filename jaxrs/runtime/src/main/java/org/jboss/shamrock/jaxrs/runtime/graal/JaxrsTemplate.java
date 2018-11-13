package org.jboss.shamrock.jaxrs.runtime.graal;

import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.cdi.BeanContainer;

/**
 * Created by bob on 7/31/18.
 */
@Template
public class JaxrsTemplate {

    public void setupIntegration(BeanContainer container) {
        ShamrockInjectorFactory.CONTAINER = container;
    }

}
