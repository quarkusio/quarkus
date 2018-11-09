package org.jboss.shamrock.camel.runtime;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.RoutesBuilder;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionInstance;

public class CamelTemplate {

    @ContextObject("camel.runtime")
    public CamelRuntime init(
                     InjectionInstance<CamelRuntime> iruntime,
                     RuntimeRegistry registry,
                     Properties properties,
                     List<InjectionInstance<RoutesBuilder>> builders) throws Exception {
        CamelRuntime runtime = iruntime.newInstance();
        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        runtime.setBuilders(builders.stream().map(InjectionInstance::newInstance).collect(Collectors.toList()));
        runtime.init();
        return runtime;
    }

    public void start(@ContextObject("camel.runtime") CamelRuntime runtime) throws Exception {
        runtime.start();
    }

}
