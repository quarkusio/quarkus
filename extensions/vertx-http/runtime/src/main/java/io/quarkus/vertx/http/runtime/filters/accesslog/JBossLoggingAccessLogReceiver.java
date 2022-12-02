/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.quarkus.vertx.http.runtime.filters.accesslog;

import org.jboss.logging.Logger;

/**
 * Access log receiver that logs messages at INFO level.
 *
 * @author Stuart Douglas
 */
public class JBossLoggingAccessLogReceiver implements AccessLogReceiver {

    public static final String DEFAULT_CATEGORY = "io.quarkus.vertx.http.accesslog";

    private final Logger logger;

    public JBossLoggingAccessLogReceiver(final String category) {
        this.logger = Logger.getLogger(category);
    }

    public JBossLoggingAccessLogReceiver() {
        this.logger = Logger.getLogger(DEFAULT_CATEGORY);
    }

    @Override
    public void logMessage(String message) {
        logger.info(message);
    }
}
