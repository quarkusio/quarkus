/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.config.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertiesConfigReader<T> {

    public static <T> PropertiesConfigReader<T> getInstance(PropertiesHandler<T> handler)
            throws PropertiesConfigReaderException {
        return getInstance(handler, (PropertyLine line) -> PropertiesConfigUtils.unrecognizedProperty(line));
    }

    public static <T> PropertiesConfigReader<T> getInstance(PropertiesHandler<T> handler,
            UnrecognizedPropertyHandler unrecognizedPropHandler) throws PropertiesConfigReaderException {
        return getInstance(handler, unrecognizedPropHandler, (PropertyLine line, int nameElement) -> {
        });
    }

    public static <T> PropertiesConfigReader<T> getInstance(PropertiesHandler<T> handler,
            UnrecognizedNameElementHandler unrecognizedChildHandler) throws PropertiesConfigReaderException {
        return getInstance(handler, (PropertyLine line) -> PropertiesConfigUtils.unrecognizedProperty(line),
                unrecognizedChildHandler);
    }

    public static <T> PropertiesConfigReader<T> getInstance(PropertiesHandler<T> handler,
            UnrecognizedPropertyHandler unrecognizedPropHandler, UnrecognizedNameElementHandler unrecognizedChildHandler)
            throws PropertiesConfigReaderException {
        return new PropertiesConfigReader<>(handler, unrecognizedPropHandler, unrecognizedChildHandler);
    }

    private final UnrecognizedPropertyHandler unrecognizedPropHandler;
    private final UnrecognizedNameElementHandler unrecognizedChildHandler;
    private final T result;
    private PropertyContext root;
    private PropertyContext current;

    protected PropertiesConfigReader(PropertiesHandler<T> handler, UnrecognizedPropertyHandler unrecognizedPropHandler,
            UnrecognizedNameElementHandler unrecognizedChildHandler) throws PropertiesConfigReaderException {
        this.unrecognizedPropHandler = unrecognizedPropHandler;
        this.unrecognizedChildHandler = unrecognizedChildHandler;
        result = handler.getTarget();
        root = new PropertyContext(null, null, 0, new String[] {}, handler);
        root.o = result;
        current = root;
    }

    public T read(Path p) throws PropertiesConfigReaderException {
        build(readLines(p));
        return result;
    }

    public T read(Properties props) throws PropertiesConfigReaderException {
        final List<PropertyLine> lines = new ArrayList<>(props.size());
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            final String name = entry.getKey().toString();
            lines.add(new PropertyLine(name, entry.getValue().toString()));
        }
        build(lines);
        return result;
    }

    public T read(Map<String, String> props) throws PropertiesConfigReaderException {
        final List<PropertyLine> lines = new ArrayList<>(props.size());
        for (Map.Entry<String, String> entry : props.entrySet()) {
            final String name = entry.getKey();
            lines.add(new PropertyLine(name, entry.getValue()));
        }
        build(lines);
        return result;
    }

    public <S> T read(Iterable<S> config, PropertyLineConverter<S> converter) throws PropertiesConfigReaderException {
        final List<PropertyLine> lines = new ArrayList<>();
        for (S src : config) {
            lines.add(converter.toPropertyLine(src));
        }
        build(lines);
        return result;
    }

    private void build(final List<PropertyLine> lines) throws PropertiesConfigReaderException {
        Collections.sort(lines);
        for (PropertyLine line : lines) {
            handleProperty(line);
        }
        while (current.parent != null) {
            current.parent.handler.setNestedOnObject(current.parent.o, current.mappedName, current.o);
            current = current.parent;
        }
    }

    private List<PropertyLine> readLines(Path p) throws PropertiesConfigReaderException {
        List<PropertyLine> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            final StringBuilder nameElBuf = new StringBuilder();
            final List<String> nameEls = new ArrayList<>(1);
            String line = reader.readLine();
            int lineI = 0;
            while (line != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    line = reader.readLine();
                    continue;
                }
                nameEls.clear();
                int i = 0;
                int equalsI = -1;
                while (i < line.length()) {
                    final char c = line.charAt(i);
                    if (c == '=') {
                        nameEls.add(nameElBuf.toString());
                        nameElBuf.setLength(0);
                        equalsI = i;
                        break;
                    }
                    if (c == '.') {
                        nameEls.add(nameElBuf.toString());
                        nameElBuf.setLength(0);
                    } else {
                        nameElBuf.append(c);
                    }
                    ++i;
                }
                if (equalsI <= 0) {
                    throw new PropertiesConfigReaderException("Line '" + line + "' does not follow format <key>=<value>");
                }
                lines.add(new PropertyLine(line, lineI++, line.substring(0, equalsI),
                        nameEls.toArray(new String[nameEls.size()]), line.substring(i + 1)));
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new PropertiesConfigReaderException("Failed read " + p, e);
        }
        return lines;
    }

    protected void handleProperty(PropertyLine line) throws PropertiesConfigReaderException {
        //System.out.println("handleProperty " + line + " " + Arrays.asList(current.nameEls));

        final int minLength = Math.min(current.nameEls.length, line.nameElements.length);
        int i = 0;
        while (i < minLength) {
            if (!current.nameEls[i].equals(line.nameElements[i])) {
                break;
            }
            ++i;
        }
        int j = current.nameEls.length;
        while (j > i) {
            current.parent.handler.setNestedOnObject(current.parent.o, current.mappedName, current.o);
            j -= current.mappedNameElements;
            current = current.parent;
        }
        j = i;
        while (j < line.nameElements.length - 1) {
            String namePart = line.getNamePart(i, j++);
            final PropertiesHandler<?> childHandler = current.handler.getNestedHandler(namePart);
            if (childHandler == null) {
                unrecognizedChildHandler.unrecognizedNameElement(line, j);
                continue;
            }
            current = new PropertyContext(current, namePart, j - i, Arrays.copyOf(line.nameElements, j), childHandler);
            current.o = childHandler.getTarget();
            i = j;
        }
        current.nestedProperty = line;
        current.nameElement = i;
        if (!current.handler.setOnObject(current)) {
            unrecognizedPropHandler.unrecognizedProperty(line);
        }
        current.nestedProperty = null;
        current.nameElement = -1;
    }
}
