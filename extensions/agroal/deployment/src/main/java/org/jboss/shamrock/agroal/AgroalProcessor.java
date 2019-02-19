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

import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.processor.DotNames;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.agroal.runtime.AbstractDataSourceProducer;
import org.jboss.shamrock.agroal.runtime.AgroalBuildTimeConfig;
import org.jboss.shamrock.agroal.runtime.AgroalRuntimeConfig;
import org.jboss.shamrock.agroal.runtime.AgroalTemplate;
import org.jboss.shamrock.agroal.runtime.DataSource;
import org.jboss.shamrock.agroal.runtime.DataSourceBuildTimeConfig;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.arc.deployment.GeneratedBeanBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.SslNativeConfigBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.deployment.util.HashUtil;

import io.agroal.api.AgroalDataSource;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    /**
     * The Agroal build time configuration.
     */
    AgroalBuildTimeConfig agroalBuildTimeConfig;

    /**
     * The Agroal runtime configuration.
     */
    AgroalRuntimeConfig agroalRuntimeConfig;

    @SuppressWarnings("unchecked")
    @Record(STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(
        RecorderContext recorder,
        AgroalTemplate template,
        BuildProducer<FeatureBuildItem> feature,
        BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
        BuildProducer<SubstrateResourceBuildItem> resource,
        BuildProducer<DataSourceDriverBuildItem> dataSourceDriver,
        SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
        BuildProducer<GeneratedClassBuildItem> generatedClass,
        BuildProducer<GeneratedBeanBuildItem> generatedBean
    ) throws Exception {
        // TODO @dmlloyd
        // Funilly enough, here the config in the map seems to be properly injected...
        feature.produce(new FeatureBuildItem(FeatureBuildItem.AGROAL));

        if (!agroalBuildTimeConfig.defaultDataSource.driver.isPresent() && agroalBuildTimeConfig.namedDataSources.isEmpty()) {
            log.warn("Agroal dependency is present but no driver has been defined for the default datasource");
            return null;
        }

        // For now, we can't push the security providers to Agroal so we need to include
        // the service file inside the image. Hopefully, we will get an entry point to
        // resolve them at build time and push them to Agroal soon.
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + io.agroal.api.security.AgroalSecurityProvider.class.getName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                io.agroal.api.security.AgroalDefaultSecurityProvider.class.getName(),
                io.agroal.api.security.AgroalKerberosSecurityProvider.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()
        ));

        // Add reflection for the drivers
        if (agroalBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, agroalBuildTimeConfig.defaultDataSource.driver.get()));

            // TODO: this will need to change to support multiple datasources but it can wait
            dataSourceDriver.produce(new DataSourceDriverBuildItem(agroalBuildTimeConfig.defaultDataSource.driver.get()));
        }
        for (Entry<String, DataSourceBuildTimeConfig> namedDataSourceEntry : agroalBuildTimeConfig.namedDataSources.entrySet()) {
            if (namedDataSourceEntry.getValue().driver.isPresent()) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, namedDataSourceEntry.getValue().driver.get()));
            }
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.AGROAL));

        // Generate the DataSourceProducer bean
        String dataSourceProducerClassName = AbstractDataSourceProducer.class.getPackage().getName() + "." + "DataSourceProducer";

        createDataSourceProducerBean(generatedClass, generatedBean, dataSourceProducerClassName);

        return new BeanContainerListenerBuildItem(template.addDataSource(
                (Class<? extends AbstractDataSourceProducer>) recorder.classProxy(dataSourceProducerClassName),
                agroalBuildTimeConfig,
                sslNativeConfig.isExplicitlyDisabled()));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureRuntimeProperties(AgroalTemplate template) {
        // TODO @dmlloyd
        // Here we have the first issue:
        // - things are working well for the default database
        // - we have the datasource1 and datasource2 elements in the map but the values are not injected
        // - as mentioned above, it doesn't seem to be an issue for the build time config I use in the above method...
        template.configureRuntimeProperties(agroalRuntimeConfig);
    }

    private void createDataSourceProducerBean(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String dataSourceProducerClassName) {
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(false, name, data));
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        };

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(dataSourceProducerClassName)
                .superClass(AbstractDataSourceProducer.class)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        if (agroalBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            MethodCreator defaultDataSourceMethodCreator = classCreator.getMethodCreator("createDefaultDataSource", AgroalDataSource.class);
            defaultDataSourceMethodCreator.addAnnotation(Singleton.class);
            defaultDataSourceMethodCreator.addAnnotation(Produces.class);
            defaultDataSourceMethodCreator.addAnnotation(Default.class);

            ResultHandle dataSourceName = defaultDataSourceMethodCreator.load(AgroalTemplate.DEFAULT_DATASOURCE_NAME);
            ResultHandle dataSourceBuildTimeConfig = defaultDataSourceMethodCreator.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDefaultBuildTimeConfig", DataSourceBuildTimeConfig.class),
                    defaultDataSourceMethodCreator.getThis());
            ResultHandle dataSourceRuntimeConfig = defaultDataSourceMethodCreator.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDefaultRuntimeConfig", Optional.class),
                    defaultDataSourceMethodCreator.getThis());

            defaultDataSourceMethodCreator.returnValue(
                    defaultDataSourceMethodCreator.invokeSpecialMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource", AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class),
                            defaultDataSourceMethodCreator.getThis(),
                            dataSourceName,
                            dataSourceBuildTimeConfig, dataSourceRuntimeConfig)
            );
        }

        for (Entry<String, DataSourceBuildTimeConfig> namedDataSourceEntry : agroalBuildTimeConfig.namedDataSources.entrySet()) {
            String namedDataSourceName = namedDataSourceEntry.getKey();

            if (!namedDataSourceEntry.getValue().driver.isPresent()) {
                log.warn("No driver defined for named datasource " + namedDataSourceName + ". Ignoring.");
                continue;
            }

            MethodCreator namedDataSourceMethodCreator = classCreator.getMethodCreator("createNamedDataSource_" + HashUtil.sha1(namedDataSourceName),
                    AgroalDataSource.class);
            namedDataSourceMethodCreator.addAnnotation(ApplicationScoped.class);
            namedDataSourceMethodCreator.addAnnotation(Produces.class);
            namedDataSourceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", namedDataSourceName) }));
            namedDataSourceMethodCreator.addAnnotation(AnnotationInstance.create(DotName.createSimple(DataSource.class.getName()), null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", namedDataSourceName) }));

            ResultHandle namedDataSourceNameRH = namedDataSourceMethodCreator.load(namedDataSourceName);
            ResultHandle namedDataSourceBuildTimeConfig = namedDataSourceMethodCreator.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getBuildTimeConfig", DataSourceBuildTimeConfig.class, String.class),
                    namedDataSourceMethodCreator.getThis(), namedDataSourceNameRH);
            ResultHandle namedDataSourceRuntimeConfig = namedDataSourceMethodCreator.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getRuntimeConfig", Optional.class, String.class),
                    namedDataSourceMethodCreator.getThis(), namedDataSourceNameRH);

            namedDataSourceMethodCreator.returnValue(
                    namedDataSourceMethodCreator.invokeSpecialMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource", AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class),
                            namedDataSourceMethodCreator.getThis(),
                            namedDataSourceNameRH,
                            namedDataSourceBuildTimeConfig, namedDataSourceRuntimeConfig)
            );
        }

        classCreator.close();
    }
}
