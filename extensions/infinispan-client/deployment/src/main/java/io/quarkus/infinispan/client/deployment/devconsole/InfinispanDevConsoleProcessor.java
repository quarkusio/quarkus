package io.quarkus.infinispan.client.deployment.devconsole;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.infinispan.client.runtime.devconsole.InfinispanClientsContainer;

public class InfinispanDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem infinispanServer(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("infinispanClient",
                new BeanLookupSupplier(InfinispanClientsContainer.class),
                this.getClass(),
                curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(InfinispanClientsContainer.class);
    }
}
