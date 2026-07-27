package io.quarkus.jdbc.runtime.deployment;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.jdbc.runtime.runtime.RuntimeJdbc;
import io.quarkus.jdbc.runtime.runtime.RuntimeJdbcDriverPlaceholder;
import io.quarkus.jdbc.runtime.runtime.RuntimeJdbcDriverResolver;
import io.quarkus.jdbc.runtime.runtime.RuntimeJdbcXADataSourcePlaceholder;

public class JdbcRuntimeProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_RUNTIME);
    }

    @BuildStep
    JdbcDriverBuildItem registerPlaceholderDriver() {
        // placeholder classes satisfying the build-time validation of the Agroal extension,
        // the actual driver is substituted at runtime by RuntimeJdbcDriverResolver
        return new JdbcDriverBuildItem(RuntimeJdbc.DB_KIND, RuntimeJdbcDriverPlaceholder.class.getName(),
                RuntimeJdbcXADataSourcePlaceholder.class.getName());
    }

    @BuildStep
    DefaultDataSourceDbKindBuildItem registerDefaultDbKind() {
        return new DefaultDataSourceDbKindBuildItem(RuntimeJdbc.DB_KIND);
    }

    @BuildStep
    void registerConfiguredDriversForReflection(final DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final var config = ConfigProvider.getConfig();
        for (final var dataSourceName : dataSourcesBuildTimeConfig.dataSources().keySet()) {
            for (final var key : DataSourceUtil.dataSourcePropertyKeys(dataSourceName, "jdbc-runtime.driver")) {
                final var driver = config.getOptionalValue(key, String.class);
                if (driver.isPresent()) {
                    // best effort for native images: the driver is a runtime choice but when the property is
                    // already visible at build time (e.g. set in application.properties) the configured class
                    // is registered for reflection; methods are needed for the XADataSource/DataSource
                    // property injection done by Agroal
                    reflectiveClass.produce(ReflectiveClassBuildItem.builder(driver.get()).methods().build());
                    break;
                }
            }
        }
    }

    @BuildStep
    AdditionalBeanBuildItem registerDriverResolver() {
        return new AdditionalBeanBuildItem.Builder()
                .addBeanClass(RuntimeJdbcDriverResolver.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable()
                .build();
    }
}
