/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A utility for converting objects into strings and strings into objects for storage in logging configurations.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("WeakerAccess")
public class PropertyValues {

    private static final int KEY = 0;
    private static final int VALUE = 1;

    /**
     * Parses a string of key/value pairs into a map.
     * <p>
     * The key/value pairs are separated by a comma ({@code ,}). The key and value are separated by an equals
     * ({@code =}).
     * </p>
     * <p>
     * If a key contains a {@code \} or an {@code =} it must be escaped by a preceding {@code \}. Example: {@code
     * key\==value,\\key=value}.
     * </p>
     * <p>
     * If a value contains a {@code \} or a {@code ,} it must be escaped by a preceding {@code \}. Example: {@code
     * key=part1\,part2,key2=value\\other}.
     * </p>
     *
     * <p>
     * If the value for a key is empty there is no trailing {@code =} after a key the will be {@code null}.
     * </p>
     *
     * @param s the string to parse
     *
     * @return a map of the key value pairs or an empty map if the string is {@code null} or empty
     */
    public static Map<String, String> stringToMap(final String s) {
        if (s == null || s.isEmpty()) return Collections.emptyMap();

        final Map<String, String> map = new LinkedHashMap<>();

        final StringBuilder key = new StringBuilder();
        final StringBuilder value = new StringBuilder();
        final char[] chars = s.toCharArray();
        int state = 0;
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            switch (state) {
                case KEY: {
                    switch (c) {
                        case '\\': {
                            // Handle escapes
                            if (chars.length > ++i) {
                                final char next = chars[i];
                                if (next == '=' || next == '\\') {
                                    key.append(next);
                                    continue;
                                }
                            }
                            throw new IllegalStateException("Escape character found at invalid position " + i + ". Only characters '=' and '\\' need to be escaped for a key.");
                        }
                        case '=': {
                            state = VALUE;
                            continue;
                        }
                        default: {
                            key.append(c);
                            continue;
                        }
                    }
                }
                case VALUE: {
                    switch (c) {
                        case '\\': {
                            // Handle escapes
                            if (chars.length > ++i) {
                                final char next = chars[i];
                                if (next == ',' || next == '\\') {
                                    value.append(next);
                                    continue;
                                }
                            }
                            throw new IllegalStateException("Escape character found at invalid position " + i + ". Only characters ',' and '\\' need to be escaped for a value.");
                        }
                        case ',': {
                            // Only add if the key isn't empty
                            if (key.length() > 0) {
                                // Add the entry
                                if (value.length() == 0) {
                                    map.put(key.toString(), null);
                                } else {
                                    map.put(key.toString(), value.toString());
                                }
                                // Clear the key
                                key.setLength(0);
                            }
                            // Clear the value
                            value.setLength(0);
                            state = KEY;
                            continue;
                        }
                        default: {
                            value.append(c);
                            continue;
                        }
                    }
                }
                default:
                    // not reachable
                    throw new IllegalStateException();
            }
        }
        // Add the last entry
        if (key.length() > 0) {
            // Add the entry
            if (value.length() == 0) {
                map.put(key.toString(), null);
            } else {
                map.put(key.toString(), value.toString());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Parses a string of key/value pairs into an {@linkplain EnumMap enum map}.
     * <p>
     * The key/value pairs are separated by a comma ({@code ,}). The key and value are separated by an equals
     * ({@code =}). The key must be a valid {@linkplain Enum#valueOf(Class, String) enum value}. For convenience the
     * case of each character will be converted to uppercase and any dashes ({@code -}) will be converted to
     * underscores ({@code _}).
     * </p>
     * <p>
     * If a value contains a {@code \} or a {@code ,} it must be escaped by a preceding {@code \}. Example: {@code
     * key=part1\,part2,key2=value\\other}.
     * </p>
     *
     * <p>
     * If the value for a key is empty there is no trailing {@code =} after a key the value will be {@code null}.
     * </p>
     *
     * @param enumType the enum type
     * @param s        the string to parse
     *
     * @return a map of the key value pairs or an empty map if the string is {@code null} or empty
     */
    public static <E extends Enum<E>> EnumMap<E, String> stringToEnumMap(final Class<E> enumType, final String s) {
        return stringToEnumMap(enumType, s, true);
    }


    /**
     * Parses a string of key/value pairs into an {@linkplain EnumMap enum map}.
     * <p>
     * The key/value pairs are separated by a comma ({@code ,}). The key and value are separated by an equals
     * ({@code =}). The key must be a valid {@linkplain Enum#valueOf(Class, String) enum value}. For convenience any
     * dashes ({@code -}) will be converted to underscores ({@code _}). If {@code convertKeyCase} is set to
     * {@code true} the case will also be converted to uppercase for each key character.
     * </p>
     * <p>
     * If a value contains a {@code \} or a {@code ,} it must be escaped by a preceding {@code \}. Example: {@code
     * key=part1\,part2,key2=value\\other}.
     * </p>
     *
     * <p>
     * If the value for a key is empty there is no trailing {@code =} after a key the value will be {@code null}.
     * </p>
     *
     * @param enumType       the enum type
     * @param s              the string to parse
     * @param convertKeyCase {@code true} if the each character from the key should be converted to uppercase,
     *                       otherwise {@code false} to keep the case as is
     *
     * @return a map of the key value pairs or an empty map if the string is {@code null} or empty
     */
    @SuppressWarnings("SameParameterValue")
    public static <E extends Enum<E>> EnumMap<E, String> stringToEnumMap(final Class<E> enumType, final String s, final boolean convertKeyCase) {
        final EnumMap<E, String> result = new EnumMap<>(enumType);
        if (s == null || s.isEmpty()) return result;

        final StringBuilder key = new StringBuilder();
        final StringBuilder value = new StringBuilder();
        final char[] chars = s.toCharArray();
        int state = 0;
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            switch (state) {
                case KEY: {
                    switch (c) {
                        case '=': {
                            state = VALUE;
                            continue;
                        }
                        case '-': {
                            key.append('_');
                            continue;
                        }
                        default: {
                            if (convertKeyCase) {
                                key.append(Character.toUpperCase(c));
                            } else {
                                key.append(c);
                            }
                            continue;
                        }
                    }
                }
                case VALUE: {
                    switch (c) {
                        case '\\': {
                            // Handle escapes
                            if (chars.length > ++i) {
                                final char next = chars[i];
                                if (next == ',' || next == '\\') {
                                    value.append(next);
                                    continue;
                                }
                            }
                            throw new IllegalStateException("Escape character found at invalid position " + i + ". Only characters ',' and '\\' need to be escaped for a value.");
                        }
                        case ',': {
                            // Only add if the key isn't empty
                            if (key.length() > 0) {
                                // Add the value
                                if (value.length() == 0) {
                                    result.put(E.valueOf(enumType, key.toString()), null);
                                } else {
                                    result.put(E.valueOf(enumType, key.toString()), value.toString());
                                }
                                // Clear the key
                                key.setLength(0);
                            }
                            // Clear the value
                            value.setLength(0);
                            state = KEY;
                            continue;
                        }
                        default: {
                            value.append(c);
                            continue;
                        }
                    }
                }
                default:
                    // not reachable
                    throw new IllegalStateException();
            }
        }
        // Add the last entry
        if (key.length() > 0) {
            // Add the value
            if (value.length() == 0) {
                result.put(E.valueOf(enumType, key.toString()), null);
            } else {
                result.put(E.valueOf(enumType, key.toString()), value.toString());
            }
        }
        return result;
    }

    /**
     * Converts a map into a string that can be parsed by {@link #stringToMap(String)}. Note that if this is an
     * {@link EnumMap} the {@link #mapToString(EnumMap)} will be used and the key will be the
     * {@linkplain Enum#name() enum name}.
     *
     * @param map the map to convert to a string
     * @param <K> the type of the key
     *
     * @return a string value for that map that can be used for configuration properties
     *
     * @see #escapeKey(StringBuilder, String)
     * @see #escapeValue(StringBuilder, String)
     */
    @SuppressWarnings("unchecked")
    public static <K> String mapToString(final Map<K, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        if (map instanceof EnumMap) {
            return mapToString((EnumMap) map);
        }
        final StringBuilder sb = new StringBuilder(map.size() * 32);
        final Iterator<Map.Entry<K, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<K, String> entry = iterator.next();
            escapeKey(sb, String.valueOf(entry.getKey()));
            sb.append('=');
            escapeValue(sb, entry.getValue());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /**
     * Converts a map into a string that can be parsed by {@link #stringToMap(String)}. The kwy will be the
     * {@linkplain Enum#name() enum name}.
     *
     * @param map the map to convert to a string
     * @param <K> the type of the key
     *
     * @return a string value for that map that can be used for configuration properties
     *
     * @see #escapeKey(StringBuilder, String)
     * @see #escapeValue(StringBuilder, String)
     */
    public static <K extends Enum<K>> String mapToString(final EnumMap<K, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(map.size() * 32);
        final Iterator<Map.Entry<K, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<K, String> entry = iterator.next();
            sb.append(entry.getKey().name());
            sb.append('=');
            escapeValue(sb, entry.getValue());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a maps key value for serialization to a string. If the key contains a {@code \} or an {@code =} it will
     * be escaped by a preceding {@code \}. Example: {@code  key\=} or {@code \\key}.
     *
     * @param sb  the string builder to append the escaped key to
     * @param key the key
     */
    public static void escapeKey(final StringBuilder sb, final String key) {
        final char[] chars = key.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            // Ensure that \ and = are escaped
            if (c == '\\') {
                final int n = i + 1;
                if (n >= chars.length) {
                    sb.append('\\').append('\\');
                } else {
                    final char next = chars[n];
                    if (next == '\\' || next == '=') {
                        // Nothing to do, already properly escaped
                        sb.append(c);
                        sb.append(next);
                        i = n;
                    } else {
                        // Now we need to escape the \
                        sb.append('\\').append('\\');
                    }
                }
            } else if (c == '=') {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
    }

    /**
     * Escapes a maps value for serialization to a string. If a value contains a {@code \} or a {@code ,} it will be
     * escaped by a preceding {@code \}. Example: {@code part1\,part2} or {@code value\\other}.
     *
     * @param sb    the string builder to append the escaped value to
     * @param value the value
     */
    public static void escapeValue(final StringBuilder sb, final String value) {
        if (value != null) {
            final char[] chars = value.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                final char c = chars[i];
                // Ensure that \ and , are escaped
                if (c == '\\') {
                    final int n = i + 1;
                    if (n >= chars.length) {
                        sb.append('\\').append('\\');
                    } else {
                        final char next = chars[n];
                        if (next == '\\' || next == ',') {
                            // Nothing to do, already properly escaped
                            sb.append(c);
                            sb.append(next);
                            i = n;
                        } else {
                            // Now we need to escape the \
                            sb.append('\\').append('\\');
                        }
                    }
                } else if (c == ',') {
                    sb.append('\\').append(c);
                } else {
                    sb.append(c);
                }
            }
        }
    }
}
