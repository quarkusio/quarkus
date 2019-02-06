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

package org.jboss.builder.diag;

import java.io.PrintStream;

import org.jboss.builder.location.Location;
import org.wildfly.common.Assert;

/**
 */
public final class Diagnostic {
    private final Level level;
    private final Location location;
    private final String format;
    private final Object[] args;
    private final Throwable thrown;

    public Diagnostic(final Level level, final Location location, final String format, final Object... args) {
        this(level, null, location, format, args);
    }

    public Diagnostic(final Level level, final Throwable thrown, final Location location, final String format, final Object... args) {
        Assert.checkNotNullParam("level", level);
        Assert.checkNotNullParam("format", format);
        Assert.checkNotNullParam("args", args);
        this.level = level;
        this.location = location;
        this.format = format;
        this.args = args.clone();
        this.thrown = thrown;
    }

    public void print(PrintStream os) {
        if (location != null) {
            os.print(location);
            os.print(": ");
        }
        os.print('[');
        os.print(level);
        os.print("]: ");
        os.printf(format, args);
        if (thrown != null) {
            os.print(": ");
            thrown.printStackTrace(os);
        }
        os.println();
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(final StringBuilder b) {
        if (location != null) {
            b.append(location).append(": ");
        }
        b.append('[').append(level).append("]: ");
        b.append(String.format(format, args));
        if (thrown != null) {
            b.append(": ").append(thrown);
        }
        return b;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public Level getLevel() {
        return level;
    }

    public enum Level {
        ERROR("error"),
        WARN("warning"),
        NOTE("note"),
        ;

        private final String name;

        Level(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
