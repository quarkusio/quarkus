package org.jboss.shamrock.deployment.steps;

import java.util.List;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.InjectionFactoryBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;
import org.jboss.shamrock.runtime.DefaultInjectionTemplate;

public class InjectionFactoryBuildStep {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    InjectionFactoryBuildItem factory(List<InjectionProviderBuildItem> providers, DefaultInjectionTemplate template) {
        if (providers.isEmpty()) {
            return new InjectionFactoryBuildItem(template.defaultFactory());
        } else if (providers.size() != 1) {
            throw new RuntimeException("At most a single Injection provider can be registered. Make sure you have not included multiple dependency injection containers in your project (e.g. Weld and Arc) " + providers);
        }
        return new InjectionFactoryBuildItem(providers.get(0).getFactory());
    }


}
