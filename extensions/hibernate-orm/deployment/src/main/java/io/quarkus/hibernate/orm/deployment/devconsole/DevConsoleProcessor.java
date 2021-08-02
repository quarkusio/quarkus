package io.quarkus.hibernate.orm.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.orm.runtime.devconsole.HibernateOrmDevConsoleInfoSupplier;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectDeploymentUnits() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("persistence", new HibernateOrmDevConsoleInfoSupplier());
    }

}
