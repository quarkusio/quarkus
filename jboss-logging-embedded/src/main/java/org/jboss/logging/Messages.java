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

package org.jboss.logging;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.Locale;

import static java.security.AccessController.doPrivileged;

/**
 * A factory class to produce message bundle implementations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Messages {

    private Messages() {
    }

    /**
     * Get a message bundle of the given type.  Equivalent to <code>{@link #getBundle(Class, java.util.Locale) getBundle}(type, Locale.getDefault())</code>.
     *
     * @param type the bundle type class
     * @param <T> the bundle type
     * @return the bundle
     */
    public static <T> T getBundle(Class<T> type) {
        return getBundle(type, LoggingLocale.getLocale());
    }

    /**
     * Get a message bundle of the given type.
     *
     * @param type the bundle type class
     * @param locale the message locale to use
     * @param <T> the bundle type
     * @return the bundle
     */
    public static <T> T getBundle(final Class<T> type, final Locale locale) {
        return doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                String language = locale.getLanguage();
                String country = locale.getCountry();
                String variant = locale.getVariant();

                Class<? extends T> bundleClass = null;
                if (variant != null && variant.length() > 0) try {
                    bundleClass = Class.forName(join(type.getName(), "$bundle", language, country, variant), true, type.getClassLoader()).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (bundleClass == null && country != null && country.length() > 0) try {
                    bundleClass = Class.forName(join(type.getName(), "$bundle", language, country, null), true, type.getClassLoader()).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (bundleClass == null && language != null && language.length() > 0) try {
                    bundleClass = Class.forName(join(type.getName(), "$bundle", language, null, null), true, type.getClassLoader()).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (bundleClass == null) try {
                    bundleClass = Class.forName(join(type.getName(), "$bundle", null, null, null), true, type.getClassLoader()).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Invalid bundle " + type + " (implementation not found)");
                }
                final Field field;
                try {
                    field = bundleClass.getField("INSTANCE");
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException("Bundle implementation " + bundleClass + " has no instance field");
                }
                try {
                    return type.cast(field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Bundle implementation " + bundleClass + " could not be instantiated", e);
                }
            }
        });
    }

    private static String join(String interfaceName, String a, String b, String c, String d) {
        final StringBuilder build = new StringBuilder();
        build.append(interfaceName).append('_').append(a);
        if (b != null && b.length() > 0) {
            build.append('_');
            build.append(b);
        }
        if (c != null && c.length() > 0) {
            build.append('_');
            build.append(c);
        }
        if (d != null && d.length() > 0) {
            build.append('_');
            build.append(d);
        }
        return build.toString();
    }
}
