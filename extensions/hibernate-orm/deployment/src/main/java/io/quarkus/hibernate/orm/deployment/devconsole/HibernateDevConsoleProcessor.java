package io.quarkus.hibernate.orm.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.runtime.devconsole.HibernateOrmDevConsoleInfoSupplier;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class HibernateDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectDeploymentUnits(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("persistence", new HibernateOrmDevConsoleInfoSupplier(),
                this.getClass(),
                curateOutcomeBuildItem);
    }

}
