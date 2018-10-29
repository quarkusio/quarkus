package org.jboss.shamrock.deployment.steps;

import java.util.List;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.InjectionFactoryBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;

public class InjectionFactoryBuildStep {

    @BuildStep
    InjectionFactoryBuildItem factory(List<InjectionProviderBuildItem> providers) {
        return new InjectionFactoryBuildItem();
    }
}
