package io.quarkus.hibernate.orm.deployment.vector;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceFeatureRequirementBuildItem;
import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;

@BuildSteps(onlyIf = HibernateVectorAvailable.class)
public class HibernateVectorDevServicesProcessor {

    @BuildStep
    DataSourceFeatureRequirementBuildItem requireVectorFeature() {
        return new DataSourceFeatureRequirementBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                DatabaseFeature.VECTOR);
    }
}
