/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.shamrock.example.test;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 */
public class LoggingConfig implements EmbeddedConfigurator {
    public Level getMinimumLevelOf(final String loggerName) {
        return loggerName.equals("javax.management") ? Level.OFF : loggerName.isEmpty() ? Level.ALL : null;
    }

    public Level getLevelOf(final String loggerName) {
        return loggerName.isEmpty() ? org.jboss.logmanager.Level.TRACE : null;
    }

    public Handler[] getHandlersOf(final String loggerName) {
        return loggerName.isEmpty() ? new Handler[] {
            new ConsoleHandler(new PatternFormatter("%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n"))
        } : NO_HANDLERS;
    }
}
