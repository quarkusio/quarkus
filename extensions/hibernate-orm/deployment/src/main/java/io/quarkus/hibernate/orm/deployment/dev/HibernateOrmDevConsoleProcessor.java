package io.quarkus.hibernate.orm.deployment.dev;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
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
    void handleMoveSql(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfoProducer,
            BuildProducer<JdbcInitialSQLGeneratorBuildItem> initialSQLGeneratorBuildItemBuildProducer,
            HibernateOrmConfig config, CurateOutcomeBuildItem curateOutcomeBuildItem) {

        DevConsoleRuntimeTemplateInfoBuildItem devConsoleRuntimeTemplateInfoBuildItem = new DevConsoleRuntimeTemplateInfoBuildItem(
                "create-ddl." + PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                new HibernateOrmDevInfoCreateDDLSupplier(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME), this.getClass(),
                curateOutcomeBuildItem);
        runtimeInfoProducer.produce(devConsoleRuntimeTemplateInfoBuildItem);
        for (Map.Entry<String, HibernateOrmConfigPersistenceUnit> entry : config.getAllPersistenceUnitConfigsAsMap()
                .entrySet()) {
            handleGenerateSqlForPu(runtimeInfoProducer, initialSQLGeneratorBuildItemBuildProducer, entry.getKey(),
                    entry.getValue().datasource.orElse(DataSourceUtil.DEFAULT_DATASOURCE_NAME), curateOutcomeBuildItem);
        }
    }

    private void handleGenerateSqlForPu(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfoProducer,
            BuildProducer<JdbcInitialSQLGeneratorBuildItem> initialSQLGeneratorBuildItemBuildProducer, String puName,
            String dsName, CurateOutcomeBuildItem curateOutcomeBuildItem) {
        DevConsoleRuntimeTemplateInfoBuildItem devConsoleRuntimeTemplateInfoBuildItem = new DevConsoleRuntimeTemplateInfoBuildItem(
                "create-ddl." + puName, new HibernateOrmDevInfoCreateDDLSupplier(puName), this.getClass(),
                curateOutcomeBuildItem);
        runtimeInfoProducer.produce(devConsoleRuntimeTemplateInfoBuildItem);
        initialSQLGeneratorBuildItemBuildProducer.produce(new JdbcInitialSQLGeneratorBuildItem(dsName, new Supplier<String>() {
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
