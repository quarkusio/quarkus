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

package org.jboss.logmanager.formatters;

import java.io.Writer;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.logmanager.PropertyValues;

/**
 * A formatter that outputs the record in XML format.
 * <p>
 * The details include;
 * </p>
 * <ul>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceClassName() source class name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceFileName() source file name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceMethodName() source method name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceLineNumber() source line number}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue"})
public class XmlFormatter extends StructuredFormatter {

    public static final String DEFAULT_NAMESPACE = "urn:jboss:logmanager:formatter:1.0";

    private final XMLOutputFactory factory = XMLOutputFactory.newFactory();

    private volatile boolean prettyPrint = false;
    private volatile boolean printNamespace = false;
    private volatile String namespaceUri;

    /**
     * Creates a new XML formatter.
     */
    public XmlFormatter() {
        namespaceUri = DEFAULT_NAMESPACE;
    }

    /**
     * Creates a new XML formatter.
     * <p>
     * If the {@code keyOverrides} is empty the default {@linkplain #DEFAULT_NAMESPACE namespace} will be used.
     * </p>
     *
     * @param keyOverrides a string representation of a map to override keys
     *
     * @see PropertyValues#stringToEnumMap(Class, String)
     */
    public XmlFormatter(final String keyOverrides) {
        super(keyOverrides);
        if (keyOverrides == null || keyOverrides.isEmpty()) {
            namespaceUri = DEFAULT_NAMESPACE;
        } else {
            namespaceUri = null;
        }
    }

    /**
     * Creates a new XML formatter.
     * <p>
     * If the {@code keyOverrides} is empty the default {@linkplain #DEFAULT_NAMESPACE namespace} will be used.
     * </p>
     *
     * @param keyOverrides a map of overrides for the default keys
     */
    public XmlFormatter(final Map<Key, String> keyOverrides) {
        super(keyOverrides);
        if (keyOverrides == null || keyOverrides.isEmpty()) {
            namespaceUri =  DEFAULT_NAMESPACE;
        } else {
            namespaceUri = null;
        }
    }

    /**
     * Indicates whether or not pretty printing is enabled.
     *
     * @return {@code true} if pretty printing is enabled, otherwise {@code false}
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    /**
     * Turns on or off pretty printing.
     *
     * @param prettyPrint {@code true} to turn on pretty printing or {@code false} to turn it off
     */
    public void setPrettyPrint(final boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    /**
     * Indicates whether or not the name space should be written on the <code>{@literal <record/>}</code>.
     *
     * @return {@code true} if the name space should be written for each record
     */
    public boolean isPrintNamespace() {
        return printNamespace;
    }

    /**
     * Turns on or off the printing of the namespace for each <code>{@literal <record/>}</code>. This is set to
     * {@code false} by default.
     *
     * @param printNamespace {@code true} if the name space should be written for each record
     */
    public void setPrintNamespace(final boolean printNamespace) {
        this.printNamespace = printNamespace;
    }

    /**
     * Returns the namespace URI used for each record if {@link #isPrintNamespace()} is {@code true}.
     *
     * @return the namespace URI, may be {@code null} if explicitly set to {@code null}
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * Sets the namespace URI used for each record if {@link #isPrintNamespace()} is {@code true}.
     *
     * @param namespaceUri the namespace to use or {@code null} if no namespace URI should be used regardless of the
     *                     {@link #isPrintNamespace()} value
     */
    public void setNamespaceUri(final String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }

    @Override
    protected Generator createGenerator(final Writer writer) throws Exception {
        final XMLStreamWriter xmlWriter;
        if (prettyPrint) {
            xmlWriter = new IndentingXmlWriter(factory.createXMLStreamWriter(writer));
        } else {
            xmlWriter = factory.createXMLStreamWriter(writer);
        }
        return new XmlGenerator(xmlWriter);
    }

    private class XmlGenerator implements Generator {
        private final XMLStreamWriter xmlWriter;

        private XmlGenerator(final XMLStreamWriter xmlWriter) {
            this.xmlWriter = xmlWriter;
        }

        @Override
        public Generator begin() throws Exception {
            writeStart(getKey(Key.RECORD));
            if (printNamespace && namespaceUri != null) {
                xmlWriter.writeDefaultNamespace(namespaceUri);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) throws Exception {
            if (value == null) {
                writeEmpty(key);
            } else {
                writeStart(key);
                for (Map.Entry<String, ?> entry : value.entrySet()) {
                    final String k = entry.getKey();
                    final Object v = entry.getValue();
                    if (v == null) {
                        writeEmpty(k);
                    } else {
                        add(k, String.valueOf(v));
                    }
                }
                writeEnd();
            }
            return this;
        }

        @Override
        public Generator add(final String key, final String value) throws Exception {
            if (value == null) {
                writeEmpty(key);
            } else {
                writeStart(key);
                xmlWriter.writeCharacters(value);
                writeEnd();
            }
            return this;
        }

        @Override
        public Generator addMetaData(final Map<String, String> metaData) throws Exception {
            for (String key : metaData.keySet()) {
                writeStart("metaData");
                xmlWriter.writeAttribute("key", key);
                final String value = metaData.get(key);
                if (value != null) {
                    xmlWriter.writeCharacters(metaData.get(key));
                }
                writeEnd();
            }
            return this;
        }

        @Override
        public Generator startObject(final String key) throws Exception {
            writeStart(key);
            return this;
        }

        @Override
        public Generator endObject() throws Exception {
            writeEnd();
            return this;
        }

        @Override
        public Generator addAttribute(final String name, final int value) throws Exception {
            return addAttribute(name, Integer.toString(value));
        }

        @Override
        public Generator addAttribute(final String name, final String value) throws Exception {
            xmlWriter.writeAttribute(name, value);
            return this;
        }

        @Override
        public Generator end() throws Exception {
            writeEnd(); // end record
            safeFlush(xmlWriter);
            safeClose(xmlWriter);
            return this;
        }

        @Override
        public boolean wrapArrays() {
            return true;
        }

        private void writeEmpty(final String name) throws XMLStreamException {
            xmlWriter.writeEmptyElement(name);
        }

        private void writeStart(final String name) throws XMLStreamException {
            xmlWriter.writeStartElement(name);
        }

        private void writeEnd() throws XMLStreamException {
            xmlWriter.writeEndElement();
        }

        private void safeFlush(final XMLStreamWriter flushable) {
            if (flushable != null) try {
                flushable.flush();
            } catch (Throwable ignore) {
            }
        }

        private void safeClose(final XMLStreamWriter closeable) {
            if (closeable != null) try {
                closeable.close();
            } catch (Throwable ignore) {
            }
        }
    }
}
