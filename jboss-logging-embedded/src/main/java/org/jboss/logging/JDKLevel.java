/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2010 Red Hat, Inc.
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

package org.jboss.logging;

import java.util.logging.Level;

/**
 * Levels used by the JDK logging back end.
 */
final class JDKLevel extends Level {

    private static final long serialVersionUID = 1L;

    protected JDKLevel(final String name, final int value) {
        super(name, value);
    }

    protected JDKLevel(final String name, final int value, final String resourceBundleName) {
        super(name, value, resourceBundleName);
    }

    public static final JDKLevel FATAL = new JDKLevel("FATAL", 1100);
    public static final JDKLevel ERROR = new JDKLevel("ERROR", 1000);
    public static final JDKLevel WARN = new JDKLevel("WARN", 900);
    @SuppressWarnings("hiding")
    public static final JDKLevel INFO = new JDKLevel("INFO", 800);
    public static final JDKLevel DEBUG = new JDKLevel("DEBUG", 500);
    public static final JDKLevel TRACE = new JDKLevel("TRACE", 400);
}
