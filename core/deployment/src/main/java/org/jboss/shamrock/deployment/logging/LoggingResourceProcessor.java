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

package io.quarkus.deployment.logging;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.jboss.logmanager.EmbeddedConfigurator;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationCustomConverterBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import io.quarkus.runtime.logging.LogConfig;
import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.runtime.logging.LevelConverter;
import io.quarkus.runtime.logging.LoggingSetupTemplate;

/**
 */
public final class LoggingResourceProcessor {

    @BuildStep
    SystemPropertyBuildItem setProperty() {
        return new SystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @BuildStep
    void setUpDefaultLevels(List<LogCategoryBuildItem> categories, Consumer<RunTimeConfigurationDefaultBuildItem> configOutput) {
        for (LogCategoryBuildItem category : categories) {
            configOutput.accept(
                new RunTimeConfigurationDefaultBuildItem(
                    "quarkus.log.categories.\"" + category.getCategory() + "\".level",
                    category.getLevel().toString()
                )
            );
        }
    }

    @BuildStep
    void miscSetup(
        Consumer<RuntimeInitializedClassBuildItem> runtimeInit,
        Consumer<GeneratedResourceBuildItem> generatedResource,
        Consumer<SubstrateSystemPropertyBuildItem> systemProp,
        Consumer<ServiceProviderBuildItem> provider
    ) {
        runtimeInit.accept(new RuntimeInitializedClassBuildItem("org.jboss.logmanager.formatters.TrueColorHolder"));
        systemProp.accept(new SubstrateSystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager"));
        provider.accept(new ServiceProviderBuildItem(EmbeddedConfigurator.class.getName(), InitialConfigurator.class.getName()));
        generatedResource.accept(
            new GeneratedResourceBuildItem(
                "META-INF/services/org.jboss.logmanager.EmbeddedConfigurator",
                InitialConfigurator.class.getName().getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupLoggingRuntimeInit(LoggingSetupTemplate setupTemplate, LogConfig log) {
        setupTemplate.initializeLogging(log);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupLoggingStaticInit(LoggingSetupTemplate setupTemplate, LogConfig log) {
        setupTemplate.initializeLogging(log);
    }

    @BuildStep
    ConfigurationCustomConverterBuildItem setUpLevelConverter() {
        return new ConfigurationCustomConverterBuildItem(
            200,
            Level.class,
            LevelConverter.class
        );
    }
}
