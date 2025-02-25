package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.flyway.FlywayDataSource;

/**
 * This class is sort of a producer for {@link Flyway}.
 *
 * It isn't a CDI producer in the literal sense, but it is marked as a bean
 * and it's {@code createFlyway} method is called at runtime in order to produce
 * the actual {@code Flyway} objects.
 *
 * CDI scopes and qualifiers are set up at build-time, which is why this class is devoid of
 * any CDI annotations
 *
 */
public class FlywayContainerProducer {

    private final FlywayRuntimeConfig flywayRuntimeConfig;
    private final FlywayBuildTimeConfig flywayBuildConfig;

    private final List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances;

    public FlywayContainerProducer(FlywayRuntimeConfig flywayRuntimeConfig, FlywayBuildTimeConfig flywayBuildConfig,
            @All List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildConfig = flywayBuildConfig;
        this.configCustomizerInstances = configCustomizerInstances;
    }

    public FlywayContainer createFlyway(DataSource dataSource, String dataSourceName, boolean hasMigrations,
            boolean createPossible) {
        FlywayDataSourceRuntimeConfig matchingRuntimeConfig = flywayRuntimeConfig.datasources().get(dataSourceName);
        FlywayDataSourceBuildTimeConfig matchingBuildTimeConfig = flywayBuildConfig.datasources().get(dataSourceName);
        final Collection<Callback> callbacks = QuarkusPathLocationScanner.callbacksForDataSource(dataSourceName);
        final Flyway flyway = new FlywayCreator(matchingRuntimeConfig, matchingBuildTimeConfig, matchingConfigCustomizers(
                configCustomizerInstances, dataSourceName)).withCallbacks(callbacks)
                .createFlyway(dataSource);
        return new FlywayContainer(flyway, matchingRuntimeConfig.baselineAtStart(), matchingRuntimeConfig.cleanAtStart(),
                matchingRuntimeConfig.cleanOnValidationError(), matchingRuntimeConfig.migrateAtStart(),
                matchingRuntimeConfig.repairAtStart(), matchingRuntimeConfig.validateAtStart(),
                dataSourceName, hasMigrations,
                createPossible);
    }

    private List<FlywayConfigurationCustomizer> matchingConfigCustomizers(
            List<InstanceHandle<FlywayConfigurationCustomizer>> configCustomizerInstances, String dataSourceName) {
        if ((configCustomizerInstances == null) || configCustomizerInstances.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlywayConfigurationCustomizer> result = new ArrayList<>();
        for (InstanceHandle<FlywayConfigurationCustomizer> instance : configCustomizerInstances) {
            Set<Annotation> qualifiers = instance.getBean().getQualifiers();
            boolean qualifierMatchesDS = false;
            boolean hasFlywayDataSourceQualifier = false;
            for (Annotation qualifier : qualifiers) {
                if (qualifier.annotationType().equals(FlywayDataSource.class)) {
                    hasFlywayDataSourceQualifier = true;
                    if (dataSourceName.equals(((FlywayDataSource) qualifier).value())) {
                        qualifierMatchesDS = true;
                        break;
                    }
                }
            }
            if (qualifierMatchesDS) {
                result.add(instance.get());
            } else if (DataSourceUtil.isDefault(dataSourceName) && !hasFlywayDataSourceQualifier) {
                // this is the case where a FlywayConfigurationCustomizer does not have a qualifier at all, therefore is applies to the default datasource
                result.add(instance.get());
            }
        }
        return result;
    }
}
