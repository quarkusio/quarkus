package io.quarkus.rest.client.reactive.deployment.devconsole;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.rest.client.reactive.runtime.devconsole.RestClientsContainer;

public class RestClientReactiveDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem devConsoleInfo(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("devRestClients",
                new BeanLookupSupplier(RestClientsContainer.class), this.getClass(), curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(RestClientsContainer.class);
    }
}
