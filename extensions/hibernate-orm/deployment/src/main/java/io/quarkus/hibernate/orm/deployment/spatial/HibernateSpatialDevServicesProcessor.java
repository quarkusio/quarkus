package io.quarkus.hibernate.orm.deployment.spatial;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceFeatureRequirementBuildItem;
import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;

@BuildSteps(onlyIf = HibernateSpatialAvailable.class)
public class HibernateSpatialDevServicesProcessor {

    @BuildStep
    DataSourceFeatureRequirementBuildItem requireSpatialFeature() {
        return new DataSourceFeatureRequirementBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                DatabaseFeature.SPATIAL);
    }
}
