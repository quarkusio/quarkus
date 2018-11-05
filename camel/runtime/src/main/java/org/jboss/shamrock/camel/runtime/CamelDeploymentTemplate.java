package org.jboss.shamrock.camel.runtime;

import java.util.Properties;

import org.jboss.shamrock.runtime.InjectionInstance;

public class CamelDeploymentTemplate {

    public void run(InjectionInstance<? extends CamelRuntime> ii,
                    SimpleLazyRegistry registry,
                    Properties properties) throws Exception {
        CamelRuntime runtime = ii.newInstance();
        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        runtime.run();
    }

}
