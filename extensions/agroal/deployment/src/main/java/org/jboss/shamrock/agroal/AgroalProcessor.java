/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.agroal;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.STATIC_INIT;

import org.jboss.logging.Logger;
import org.jboss.shamrock.agroal.runtime.DataSourceConfig;
import org.jboss.shamrock.agroal.runtime.DataSourceProducer;
import org.jboss.shamrock.agroal.runtime.DataSourceTemplate;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.SslNativeConfigBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    /**
     * The datasource configuration
     */
    DataSourceConfig datasource;

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(false, DataSourceProducer.class);
    }

    @Record(STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(
        BuildProducer<FeatureBuildItem> feature,
        BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
        BuildProducer<DataSourceDriverBuildItem> datasourceDriver,
        SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
        DataSourceTemplate template
    ) throws Exception {
        if (! datasource.url.isPresent() || ! datasource.driver.isPresent()) {
            log.warn("Agroal extension was included in build however no data source URL and/or driver class has been defined");
            return null;
        }

        feature.produce(new FeatureBuildItem(FeatureBuildItem.AGROAL));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()
        ));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, datasource.driver.get()));

        datasourceDriver.produce(new DataSourceDriverBuildItem(datasource.driver.get()));

        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.AGROAL));

        return new BeanContainerListenerBuildItem(template.addDatasource(datasource, sslNativeConfig.isExplicitlyDisabled()));
    }

}
