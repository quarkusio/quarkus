package io.quarkus.hibernate.orm.deployment.dev;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfoSupplier;

@BuildSteps(onlyIf = { HibernateOrmEnabled.class, IsDevelopment.class })
@Deprecated // Only useful for the legacy Dev UI
public class HibernateOrmDevConsoleProcessor {

    @BuildStep
    public DevConsoleRuntimeTemplateInfoBuildItem exposeInfo(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("persistence", new HibernateOrmDevInfoSupplier(),
                this.getClass(),
                curateOutcomeBuildItem);
    }

}
