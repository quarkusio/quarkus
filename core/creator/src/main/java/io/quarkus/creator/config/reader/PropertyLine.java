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

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyLine implements Comparable<PropertyLine> {

    protected final String line;
    protected final int index;
    protected final String name;
    protected final String[] nameElements;
    protected final String value;

    public PropertyLine(String name, String value) {
        this(null, -1, name, name.split("\\."), value);
    }

    public PropertyLine(String line, int index, String name, String[] nameElements, String value) {
        this.line = line;
        this.index = index;
        this.name = name;
        this.nameElements = nameElements;
        this.value = value;
    }

    public String getLine() {
        return line;
    }

    public int getIndex() {
        return index;
    }

    public String[] getNameElements() {
        return nameElements;
    }

    public String getName() {
        return name;
    }

    public String getRelativeName(int nameElementIndex) {
        final int lastIndex = nameElements.length - 1;
        if (nameElementIndex == lastIndex) {
            return nameElements[lastIndex];
        }
        if (nameElementIndex < 0 || nameElementIndex > lastIndex) {
            throw new IndexOutOfBoundsException(
                    "Name element index has to be in range from 0 to " + lastIndex + " but was " + nameElementIndex);
        }
        return getNamePart(nameElementIndex, lastIndex);
    }

    protected String getNamePart(int startI, int endI) {
        if (startI == endI) {
            return nameElements[startI];
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(nameElements[startI++]);
        while (startI <= endI) {
            buf.append('.').append(nameElements[startI++]);
        }
        return buf.toString();
    }

    public String getLastNameElement() {
        return nameElements[nameElements.length - 1];
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (index < 0) {
            return name + '=' + value;
        }
        return index + ") " + line;
    }

    @Override
    public int compareTo(PropertyLine o) {
        return name.compareTo(o.name);
    }
}
