package org.jboss.shamrock.camel.runtime;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.RoutesBuilder;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.runtime.Template;

@Template
public class CamelTemplate {

    public CamelRuntime init(
                     RuntimeValue<?> iruntime,
                     RuntimeRegistry registry,
                     Properties properties,
                     List<RuntimeValue<?>> builders) {
        CamelRuntime runtime = CamelRuntime.class.cast(iruntime.getValue());
        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        runtime.setBuilders(builders.stream()
                .map(RuntimeValue::getValue)
                .map(RoutesBuilder.class::cast)
                .collect(Collectors.toList()));
        runtime.init();
        return runtime;
    }

    public void start(CamelRuntime runtime) throws Exception {
        runtime.start();
    }

}
