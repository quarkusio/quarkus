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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.AbstractDataSourceProducer;
import io.quarkus.agroal.runtime.AgroalBuildTimeConfig;
import io.quarkus.agroal.runtime.AgroalRuntimeConfig;
import io.quarkus.agroal.runtime.AgroalTemplate;
import io.quarkus.agroal.runtime.DataSource;
import io.quarkus.agroal.runtime.DataSourceBuildTimeConfig;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final Set<DotName> UNREMOVABLE_BEANS = new HashSet<>(Arrays.asList(
            DotName.createSimple(AbstractDataSourceProducer.class.getName()),
            DotName.createSimple(javax.sql.DataSource.class.getName())));

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
            BuildProducer<GeneratedBeanBuildItem> generatedBean) throws Exception {
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

        // Add reflection for the drivers
        if (agroalBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false, agroalBuildTimeConfig.defaultDataSource.driver.get()));

            // TODO: this will need to change to support multiple datasources but it can wait
            dataSourceDriver.produce(new DataSourceDriverBuildItem(agroalBuildTimeConfig.defaultDataSource.driver.get()));
        }
        for (Entry<String, DataSourceBuildTimeConfig> namedDataSourceEntry : agroalBuildTimeConfig.namedDataSources
                .entrySet()) {
            if (namedDataSourceEntry.getValue().driver.isPresent()) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, false, namedDataSourceEntry.getValue().driver.get()));
            }
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.AGROAL));

        // Generate the DataSourceProducer bean
        String dataSourceProducerClassName = AbstractDataSourceProducer.class.getPackage().getName() + "."
                + "DataSourceProducer";

        createDataSourceProducerBean(generatedBean, dataSourceProducerClassName);

        return new BeanContainerListenerBuildItem(template.addDataSource(
                (Class<? extends AbstractDataSourceProducer>) recorder.classProxy(dataSourceProducerClassName),
                agroalBuildTimeConfig,
                sslNativeConfig.isExplicitlyDisabled()));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureRuntimeProperties(AgroalTemplate template,
            BuildProducer<DataSourceInitializedBuildItem> dataSourceInitialized) {
        if (!agroalBuildTimeConfig.defaultDataSource.driver.isPresent() && agroalBuildTimeConfig.namedDataSources.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        // TODO @dmlloyd
        // Here we have the first issue:
        // - things are working well for the default database
        // - we have the datasource1 and datasource2 elements in the map but the values are not injected
        // - as mentioned above, it doesn't seem to be an issue for the build time config I use in the above method...
        template.configureRuntimeProperties(agroalRuntimeConfig);

        dataSourceInitialized.produce(new DataSourceInitializedBuildItem());
    }

    @BuildStep
    UnremovableBeanBuildItem markBeansAsUnremovable() {
        return new UnremovableBeanBuildItem(beanInfo -> {
            Set<Type> types = beanInfo.getTypes();
            for (Type t : types) {
                if (UNREMOVABLE_BEANS.contains(t.name())) {
                    return true;
                }
            }

            return false;
        });
    }

    /**
     * Create a producer bean managing the lifecycle of the datasources.
     * <p>
     * Build time and runtime configuration are both injected into this bean.
     */
    private void createDataSourceProducerBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String dataSourceProducerClassName) {
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        };

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(dataSourceProducerClassName)
                .superClass(AbstractDataSourceProducer.class)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        if (agroalBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            MethodCreator defaultDataSourceMethodCreator = classCreator.getMethodCreator("createDefaultDataSource",
                    AgroalDataSource.class);
            defaultDataSourceMethodCreator.addAnnotation(Singleton.class);
            defaultDataSourceMethodCreator.addAnnotation(Produces.class);
            defaultDataSourceMethodCreator.addAnnotation(Default.class);

            ResultHandle dataSourceName = defaultDataSourceMethodCreator.load(AgroalTemplate.DEFAULT_DATASOURCE_NAME);
            ResultHandle dataSourceBuildTimeConfig = defaultDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDefaultBuildTimeConfig",
                            DataSourceBuildTimeConfig.class),
                    defaultDataSourceMethodCreator.getThis());
            ResultHandle dataSourceRuntimeConfig = defaultDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDefaultRuntimeConfig", Optional.class),
                    defaultDataSourceMethodCreator.getThis());

            defaultDataSourceMethodCreator.returnValue(
                    defaultDataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class),
                            defaultDataSourceMethodCreator.getThis(),
                            dataSourceName,
                            dataSourceBuildTimeConfig, dataSourceRuntimeConfig));
        }

        for (Entry<String, DataSourceBuildTimeConfig> namedDataSourceEntry : agroalBuildTimeConfig.namedDataSources
                .entrySet()) {
            String namedDataSourceName = namedDataSourceEntry.getKey();

            if (!namedDataSourceEntry.getValue().driver.isPresent()) {
                log.warn("No driver defined for named datasource " + namedDataSourceName + ". Ignoring.");
                continue;
            }

            MethodCreator namedDataSourceMethodCreator = classCreator.getMethodCreator(
                    "createNamedDataSource_" + HashUtil.sha1(namedDataSourceName),
                    AgroalDataSource.class);
            namedDataSourceMethodCreator.addAnnotation(ApplicationScoped.class);
            namedDataSourceMethodCreator.addAnnotation(Produces.class);
            namedDataSourceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", namedDataSourceName) }));
            namedDataSourceMethodCreator
                    .addAnnotation(AnnotationInstance.create(DotName.createSimple(DataSource.class.getName()), null,
                            new AnnotationValue[] { AnnotationValue.createStringValue("value", namedDataSourceName) }));

            ResultHandle namedDataSourceNameRH = namedDataSourceMethodCreator.load(namedDataSourceName);
            ResultHandle namedDataSourceBuildTimeConfig = namedDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getBuildTimeConfig",
                            DataSourceBuildTimeConfig.class, String.class),
                    namedDataSourceMethodCreator.getThis(), namedDataSourceNameRH);
            ResultHandle namedDataSourceRuntimeConfig = namedDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getRuntimeConfig", Optional.class,
                            String.class),
                    namedDataSourceMethodCreator.getThis(), namedDataSourceNameRH);

            namedDataSourceMethodCreator.returnValue(
                    namedDataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class),
                            namedDataSourceMethodCreator.getThis(),
                            namedDataSourceNameRH,
                            namedDataSourceBuildTimeConfig, namedDataSourceRuntimeConfig));
        }

        classCreator.close();
    }
}
