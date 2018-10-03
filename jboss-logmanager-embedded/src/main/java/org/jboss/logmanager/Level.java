/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager;

/**
 * Log4j-like levels.
 */
public final class Level extends java.util.logging.Level {
    private static final long serialVersionUID = 491981186783136939L;

    protected Level(final String name, final int value) {
        super(name, value);
    }

    protected Level(final String name, final int value, final String resourceBundleName) {
        super(name, value, resourceBundleName);
    }

    public static final Level FATAL = new Level("FATAL", 1100);
    public static final Level ERROR = new Level("ERROR", 1000);
    public static final Level WARN = new Level("WARN", 900);
    public static final Level INFO = new Level("INFO", 800);
    public static final Level DEBUG = new Level("DEBUG", 500);
    public static final Level TRACE = new Level("TRACE", 400);
}
