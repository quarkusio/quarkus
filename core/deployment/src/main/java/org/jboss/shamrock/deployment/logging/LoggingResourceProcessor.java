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

package org.jboss.shamrock.deployment.logging;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.ConfigurationCustomConverterBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.LogCategoryBuildItem;
import org.jboss.shamrock.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import org.jboss.shamrock.deployment.builditem.SystemPropertyBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ServiceProviderBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import org.jboss.shamrock.runtime.logging.InitialConfigurator;
import org.jboss.shamrock.runtime.logging.LevelConverter;
import org.jboss.shamrock.runtime.logging.LogConfig;
import org.jboss.shamrock.runtime.logging.LoggingSetupTemplate;

/**
 */
public final class LoggingResourceProcessor {

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.jboss.threads", "JBoss Threads version"));
    }
    
    @BuildStep
    SystemPropertyBuildItem setProperty() {
        return new SystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @BuildStep
    void setUpDefaultLevels(List<LogCategoryBuildItem> categories, Consumer<RunTimeConfigurationDefaultBuildItem> configOutput) {
        for (LogCategoryBuildItem category : categories) {
            configOutput.accept(
                new RunTimeConfigurationDefaultBuildItem(
                    "shamrock.log.categories.\"" + category.getCategory() + "\".level",
                    category.getLevel().toString()
                    )
            );
        }
    }

    @BuildStep
    void miscSetup(
        Consumer<RuntimeInitializedClassBuildItem> runtimeInit,
        Consumer<SubstrateSystemPropertyBuildItem> systemProp,
        Consumer<ServiceProviderBuildItem> provider
    ) {
        runtimeInit.accept(new RuntimeInitializedClassBuildItem("org.jboss.logmanager.formatters.TrueColorHolder"));
        systemProp.accept(new SubstrateSystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager"));
        provider.accept(new ServiceProviderBuildItem(EmbeddedConfigurator.class.getName(), InitialConfigurator.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupLoggingRuntimeInit(LoggingSetupTemplate setupTemplate, LogConfig log, List<LogCleanupFilterBuildItem> filters) {
        setupTemplate.initializeLogging(log, filters.stream().map(LogCleanupFilterBuildItem::getFilterElement).collect(Collectors.toList()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupLoggingStaticInit(LoggingSetupTemplate setupTemplate, LogConfig log, List<LogCleanupFilterBuildItem> filters) {
        setupTemplate.initializeLogging(log, filters.stream().map(LogCleanupFilterBuildItem::getFilterElement).collect(Collectors.toList()));
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
