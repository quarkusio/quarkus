package org.jboss.shamrock.jaxrs.runtime.graal;

import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;

/**
 * Created by bob on 7/31/18.
 */
public class JaxrsTemplate {

    public void setupIntegration(@ContextObject("bean.container") BeanContainer container) {
        ShamrockInjectorFactory.CONTAINER = container;
    }

}
