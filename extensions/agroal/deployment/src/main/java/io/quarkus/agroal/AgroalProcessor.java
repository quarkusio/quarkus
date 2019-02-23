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

package io.quarkus.agroal;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSourceProducer;
import io.quarkus.agroal.runtime.DataSourceTemplate;
import io.quarkus.agroal.runtime.DatasourceConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    /**
     * The datasource configuration
     */
    DatasourceConfig datasource;

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(false, DataSourceProducer.class);
    }

    @Record(STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<DataSourceDriverBuildItem> datasourceDriver,
            SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourceTemplate template) throws Exception {
        if (!datasource.url.isPresent() || !datasource.driver.isPresent()) {
            log.warn("Agroal extension was included in build however no data source URL and/or driver class has been defined");
            return null;
        }

        feature.produce(new FeatureBuildItem(FeatureBuildItem.AGROAL));

        // For now, we can't push the security providers to Agroal so we need to include
        // the service file inside the image. Hopefully, we will get an entry point to
        // resolve them at build time and push them to Agroal soon.
        resource.produce(new SubstrateResourceBuildItem(
                "META-INF/services/" + io.agroal.api.security.AgroalSecurityProvider.class.getName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                io.agroal.api.security.AgroalDefaultSecurityProvider.class.getName(),
                io.agroal.api.security.AgroalKerberosSecurityProvider.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, datasource.driver.get()));

        datasourceDriver.produce(new DataSourceDriverBuildItem(datasource.driver.get()));

        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.AGROAL));

        return new BeanContainerListenerBuildItem(template.addDatasource(datasource, sslNativeConfig.isExplicitlyDisabled()));
    }

}
