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

package org.jboss.shamrock.runner;

import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 * Embedded configurator that can be used to configure logging in a similar
 * manner to the auto generated configurator.
 */
public class RuntimeLoggingConfigurator implements EmbeddedConfigurator {

    static final Config config = ConfigProvider.getConfig();

    @Override
    public Level getMinimumLevelOf(final String loggerName) {
        return Level.ALL;
    }

    @Override
    public Level getLevelOf(final String loggerName) {
        Config config = getConfig();
        Optional<String> level = config.getOptionalValue("shamrock.log.category." + loggerName + ".level", String.class);
        if (level.isPresent()) {
            return Level.parse(level.get());
        }
        level = config.getOptionalValue("shamrock.log.level", String.class);
        if (level.isPresent()) {
            switch (level.get()) {
                case "FATAL":
                    return org.jboss.logmanager.Level.FATAL;
                case "ERROR":
                    return org.jboss.logmanager.Level.ERROR;
                case "WARN":
                    return org.jboss.logmanager.Level.WARN;
                case "INFO":
                    return org.jboss.logmanager.Level.INFO;
                case "DEBUG":
                    return org.jboss.logmanager.Level.DEBUG;
                case "TRACE":
                    return org.jboss.logmanager.Level.TRACE;
                default:
                    return org.jboss.logmanager.Level.parse((level.get()));
            }
        }
        return Level.INFO;
    }

    @Override
    public Handler[] getHandlersOf(final String loggerName) {
        Config config = getConfig();
        String format = config.getOptionalValue("shamrock.log.console.format", String.class)
                .orElse("%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n");
        boolean color = config.getOptionalValue("shamrock.log.console.format", Boolean.class)
                .orElse(true);

        if (color) {
            return loggerName.isEmpty() ? new Handler[]{
                    new ConsoleHandler(new ColorPatternFormatter(format))
            } : NO_HANDLERS;
        } else {
            return loggerName.isEmpty() ? new Handler[]{
                    new ConsoleHandler(new PatternFormatter(format))
            } : NO_HANDLERS;
        }
    }

    static Config getConfig() {
        if (Thread.currentThread().getContextClassLoader() == null) {
            return config;
        } else {
            return ConfigProvider.getConfig();
        }
    }

}
