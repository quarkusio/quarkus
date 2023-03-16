package io.quarkus.hibernate.orm.deployment.dev;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfoCreateDDLSupplier;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfoSupplier;

@BuildSteps(onlyIf = { HibernateOrmEnabled.class, IsDevelopment.class })
public class HibernateOrmDevConsoleProcessor {

    @BuildStep
    public DevConsoleRuntimeTemplateInfoBuildItem collectDeploymentUnits(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("persistence", new HibernateOrmDevInfoSupplier(),
                this.getClass(),
                curateOutcomeBuildItem);
    }

    @BuildStep
    void handleInitialSql(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfoProducer,
            BuildProducer<JdbcInitialSQLGeneratorBuildItem> initialSQLGeneratorBuildItemBuildProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            String dsName = puDescriptor.getConfig().getDataSource().orElse(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
            DevConsoleRuntimeTemplateInfoBuildItem devConsoleRuntimeTemplateInfoBuildItem = new DevConsoleRuntimeTemplateInfoBuildItem(
                    "create-ddl." + puName, new HibernateOrmDevInfoCreateDDLSupplier(puName), this.getClass(),
                    curateOutcomeBuildItem);
            runtimeInfoProducer.produce(devConsoleRuntimeTemplateInfoBuildItem);
            initialSQLGeneratorBuildItemBuildProducer
                    .produce(new JdbcInitialSQLGeneratorBuildItem(dsName, new Supplier<String>() {
                        @Override
                        public String get() {
                            return DevConsoleManager.getTemplateInfo()
                                    .get(devConsoleRuntimeTemplateInfoBuildItem.getGroupId() + "."
                                            + devConsoleRuntimeTemplateInfoBuildItem.getArtifactId())
                                    .get(devConsoleRuntimeTemplateInfoBuildItem.getName()).toString();
                        }
                    }));
        }
    }

}
